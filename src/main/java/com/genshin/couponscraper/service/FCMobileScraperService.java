package com.genshin.couponscraper.service;

import com.genshin.couponscraper.model.CouponResponse;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FCMobileScraperService {
    
    private static final Logger logger = LoggerFactory.getLogger(FCMobileScraperService.class);
    private static final String FC_MOBILE_URL = "https://www.fcmobileforum.com/fcmobile-redeem-codes";
    private static final int TIMEOUT_MS = 20000;
    private static final int MAX_RETRIES = 2;
    
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6,20}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})(?:st|nd|rd|th)?\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    
    // Blocklist for common false positives
    private static final Set<String> BLOCKLIST = new HashSet<>(Arrays.asList(
        "REWARD", "REWARDS", "PACK", "PACKS", "GEMS", "COIN", "COINS", "PLAYER", "PLAYERS",
        "STANDARD", "ANNIVERSARY", "LIMITED", "ITEM", "ITEMS", "CARD", "CARDS", "ACTIVE",
        "EXPIRED", "CODE", "CODES", "REDEEM", "BUTTON", "HOME", "MORE", "CLOSE", "MOBILE",
        "TRUE", "FALSE", "LABEL", "PAGE", "SECTION", "NAVBAR", "MENU", "FOOTER", "HEADER",
        "COPY", "HERE", "OCTOBER", "SEPTEMBER", "AUGUST", "JANUARY", "MARCH", "MAY",
        "POINTS", "RANK", "ICONS", "FESTIVAL", "SHANGHAI", "TICKETS"
    ));
    
    public List<CouponResponse> getActiveCoupons() {
        logger.info("Fetching FC Mobile codes from: {}", FC_MOBILE_URL);
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Attempt {} of {}", attempt, MAX_RETRIES);
                
                List<CouponResponse> codes = scrapeWithHtmlUnit();
                if (!codes.isEmpty()) {
                    logger.info("Successfully fetched {} FC Mobile codes", codes.size());
                    return codes;
                }
                
                // Fallback to JSoup if HtmlUnit fails
                logger.info("HtmlUnit returned no codes, trying JSoup fallback...");
                codes = scrapeWithJSoup();
                if (!codes.isEmpty()) {
                    logger.info("JSoup fallback found {} FC Mobile codes", codes.size());
                    return codes;
                }
                
            } catch (Exception e) {
                logger.warn("Attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        logger.error("Failed to fetch FC Mobile codes after {} attempts", MAX_RETRIES);
        return new ArrayList<>();
    }
    
    private List<CouponResponse> scrapeWithHtmlUnit() {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            // Configure for production environment
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setDownloadImages(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setTimeout(TIMEOUT_MS);
            webClient.getOptions().setUseInsecureSSL(true);
            
            // Set user agent for better compatibility
            webClient.addRequestHeader("User-Agent", 
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");
            
            HtmlPage page = webClient.getPage(FC_MOBILE_URL);
            webClient.waitForBackgroundJavaScript(3000); // Reduced wait time for production
            
            String pageSource = page.asXml();
            Document doc = Jsoup.parse(pageSource);
            
            return extractCodesFromDocument(doc);
            
        } catch (Exception e) {
            logger.warn("HtmlUnit scraping failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<CouponResponse> scrapeWithJSoup() {
        try {
            Document doc = Jsoup.connect(FC_MOBILE_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_MS)
                    .get();
            
            return extractCodesFromDocument(doc);
            
        } catch (Exception e) {
            logger.warn("JSoup scraping failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<CouponResponse> extractCodesFromDocument(Document doc) {
        List<CouponResponse> activeCodes = new ArrayList<>();
        String fullText = doc.text();
        
        logger.debug("Parsing document for active codes...");
        
        // Strategy 1: Look for active codes with COPY button (indicates active status)
        // Pattern: "Reward: ... Date CODE COPY" or "Date CODE COPY"
        Pattern activeWithCopyPattern = Pattern.compile(
            "(?i)(?:reward:\\s*([^\\n]*?)\\s*)?" +  // Optional reward
            "(\\d{1,2}(?:st|nd|rd|th)?\\s+\\w+)\\s+" +  // Date
            "([A-Z0-9]{6,20})\\s+" +  // Code
            "COPY",  // COPY button indicates active
            Pattern.MULTILINE
        );
        
        Matcher copyMatcher = activeWithCopyPattern.matcher(fullText);
        while (copyMatcher.find()) {
            String reward = copyMatcher.group(1);
            String date = copyMatcher.group(2);
            String code = copyMatcher.group(3);
            
            if (isValidCode(code)) {
                String cleanReward = (reward != null && !reward.trim().isEmpty()) ? 
                    reward.trim() : "Unknown reward";
                
                CouponResponse coupon = new CouponResponse(code, cleanReward, date, "Active", "Global");
                activeCodes.add(coupon);
                logger.debug("Found active code with COPY: {} | Reward: {} | Date: {}", 
                    code, cleanReward, date);
            }
        }
        
        // Strategy 2: Look for codes that are NOT followed by "Expired"
        // Split text into sections and analyze each
        String[] sections = fullText.split("(?i)(?=\\d{1,2}(?:st|nd|rd|th)?\\s+\\w+)");
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;
            
            // Skip sections that contain "Expired"
            if (section.toLowerCase().contains("expired")) {
                continue;
            }
            
            // Look for codes in this section
            Pattern codeInSectionPattern = Pattern.compile("\\b([A-Z0-9]{6,20})\\b");
            Matcher codeMatcher = codeInSectionPattern.matcher(section);
            
            while (codeMatcher.find()) {
                String code = codeMatcher.group(1);
                
                if (!isValidCode(code)) continue;
                
                // Check if already found
                boolean alreadyExists = activeCodes.stream()
                    .anyMatch(c -> c.getCode().equals(code));
                if (alreadyExists) continue;
                
                // Extract reward and date from this section
                String reward = extractRewardFromSection(section, code);
                String date = extractDateFromSection(section);
                
                CouponResponse coupon = new CouponResponse(code, reward, date, "Active", "Global");
                activeCodes.add(coupon);
                logger.debug("Found active code in section: {} | Reward: {} | Date: {}", 
                    code, reward, date);
            }
        }
        
        // Strategy 3: Look for structured reward blocks
        // "Reward: ... Date" followed by code
        Pattern rewardBlockPattern = Pattern.compile(
            "(?i)reward:\\s*([^\\n]*?)\\s*" +  // Reward text
            "(\\d{1,2}(?:st|nd|rd|th)?\\s+\\w+)",  // Date
            Pattern.MULTILINE
        );
        
        Matcher rewardMatcher = rewardBlockPattern.matcher(fullText);
        while (rewardMatcher.find()) {
            String reward = rewardMatcher.group(1).trim();
            String date = rewardMatcher.group(2);
            
            // Look for code after this reward block
            int endPos = rewardMatcher.end();
            String afterReward = fullText.substring(endPos, Math.min(endPos + 100, fullText.length()));
            
            // Skip if this block contains "Expired"
            if (afterReward.toLowerCase().contains("expired")) {
                continue;
            }
            
            Pattern codeAfterRewardPattern = Pattern.compile("\\b([A-Z0-9]{6,20})\\b");
            Matcher codeAfterMatcher = codeAfterRewardPattern.matcher(afterReward);
            
            if (codeAfterMatcher.find()) {
                String code = codeAfterMatcher.group(1);
                
                if (isValidCode(code)) {
                    // Check if already found
                    boolean alreadyExists = activeCodes.stream()
                        .anyMatch(c -> c.getCode().equals(code));
                    
                    if (!alreadyExists) {
                        CouponResponse coupon = new CouponResponse(code, reward, date, "Active", "Global");
                        activeCodes.add(coupon);
                        logger.debug("Found code after reward block: {} | Reward: {} | Date: {}", 
                            code, reward, date);
                    }
                }
            }
        }
        
        // Remove duplicates and sort
        Map<String, CouponResponse> uniqueCodes = new LinkedHashMap<>();
        for (CouponResponse code : activeCodes) {
            uniqueCodes.put(code.getCode(), code);
        }
        
        logger.info("Found {} unique active codes", uniqueCodes.size());
        
        return uniqueCodes.values().stream()
                .sorted((a, b) -> {
                    LocalDate dateA = parseDate(a.getDate());
                    LocalDate dateB = parseDate(b.getDate());
                    if (dateA != null && dateB != null) {
                        return dateB.compareTo(dateA);
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }
    
    private String extractRewardFromSection(String section, String code) {
        // Look for reward keyword
        String lowerSection = section.toLowerCase();
        int rewardIndex = lowerSection.indexOf("reward");
        
        if (rewardIndex >= 0) {
            // Extract text after "reward:"
            String afterReward = section.substring(rewardIndex);
            int colonIndex = afterReward.indexOf(":");
            if (colonIndex >= 0) {
                String rewardText = afterReward.substring(colonIndex + 1).trim();
                
                // Clean up the reward text
                rewardText = rewardText.replaceAll("\\b" + code + "\\b", "").trim();
                rewardText = rewardText.replaceAll("\\s+", " ");
                
                if (!rewardText.isEmpty() && rewardText.length() < 100) {
                    return rewardText;
                }
            }
        }
        
        // Fallback: look for common reward patterns
        if (section.toLowerCase().contains("gems") || section.toLowerCase().contains("pack")) {
            String[] words = section.split("\\s+");
            StringBuilder reward = new StringBuilder();
            
            for (String word : words) {
                if (word.toLowerCase().contains("gem") || 
                    word.toLowerCase().contains("pack") || 
                    word.toLowerCase().contains("point") ||
                    word.matches("\\d+")) {
                    reward.append(word).append(" ");
                }
            }
            
            String result = reward.toString().trim();
            if (!result.isEmpty()) {
                return result;
            }
        }
        
        return "Unknown reward";
    }
    
    private String extractDateFromSection(String section) {
        Matcher dateMatcher = DATE_PATTERN.matcher(section);
        if (dateMatcher.find()) {
            return dateMatcher.group(0);
        }
        return "Unknown";
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "Unknown".equals(dateStr)) {
            return null;
        }
        
        try {
            String clean = dateStr.replaceAll("(?:st|nd|rd|th)", "");
            String[] patterns = {"d MMMM", "d MMM", "dd MMMM", "dd MMM"};
            int currentYear = LocalDate.now().getYear();
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return LocalDate.parse(clean, formatter).withYear(currentYear);
                } catch (Exception ignored) {
                    // Try next pattern
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to parse date: {}", dateStr);
        }
        
        return null;
    }
    
    private boolean isValidCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        
        // Quick length check
        if (code.length() < 6 || code.length() > 20) {
            return false;
        }
        
        // Quick blocklist check
        if (BLOCKLIST.contains(code.toUpperCase())) {
            return false;
        }
        
        // Pattern check
        if (!CODE_PATTERN.matcher(code).matches()) {
            return false;
        }
        
        // Skip purely numeric codes
        if (code.matches("\\d{6,}")) {
            return false;
        }
        
        // Must have at least 2 letters
        long letterCount = code.chars().filter(Character::isLetter).count();
        if (letterCount < 2) {
            return false;
        }
        
        // Additional validation: Skip common false positives
        if (code.matches("^(COPY|HERE|MORE|PAGE|HOME|MENU|EXPIRED).*")) {
            return false;
        }
        
        return true;
    }
}