package com.genshin.couponscraper.service;

import com.genshin.couponscraper.model.CouponResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HonkaiStarRailScraperService {
    
    private static final Logger logger = LoggerFactory.getLogger(HonkaiStarRailScraperService.class);
    private static final String HSR_WIKI_URL = "https://honkai-star-rail.fandom.com/wiki/Redemption_Code";
    private static final int TIMEOUT_MS = 10000;
    
    public List<CouponResponse> getActiveCoupons() {
        List<CouponResponse> coupons = new ArrayList<>();
        
        try {
            logger.info("Fetching Honkai Star Rail redemption codes from: {}", HSR_WIKI_URL);
            
            Document doc = Jsoup.connect(HSR_WIKI_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_MS)
                    .get();
            
            // Find the Active Codes table
            Elements tables = doc.select("table.wikitable");
            
            for (Element table : tables) {
                Elements rows = table.select("tbody tr");
                
                for (Element row : rows) {
                    Elements cells = row.select("td");
                    
                    if (cells.size() >= 4) {
                        try {
                            CouponResponse coupon = parseCouponRow(cells);
                            if (coupon != null && isActiveCode(cells)) {
                                coupons.add(coupon);
                                logger.debug("Parsed HSR coupon: {}", coupon);
                            }
                        } catch (Exception e) {
                            logger.warn("Error parsing HSR coupon row: {}", e.getMessage());
                        }
                    }
                }
            }
            
            logger.info("Successfully scraped {} active Honkai Star Rail coupons", coupons.size());
            
        } catch (IOException e) {
            logger.error("Failed to fetch Honkai Star Rail coupons: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while scraping Honkai Star Rail coupons: {}", e.getMessage());
        }
        
        return coupons;
    }
    
    private CouponResponse parseCouponRow(Elements cells) {
        try {
            // Extract code from first cell
            String code = extractCode(cells.get(0));
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            
            // Extract server (usually Global for HSR)
            String server = cells.size() > 1 ? cells.get(1).text().trim() : "Global";
            if (server.isEmpty()) {
                server = "Global";
            }
            
            // Extract rewards
            String reward = extractRewards(cells.get(cells.size() >= 3 ? 2 : 1));
            
            // Extract date and status
            String dateStatus = cells.get(cells.size() - 1).text().trim();
            String date = extractDate(dateStatus);
            String status = determineStatus(dateStatus);
            
            return new CouponResponse(code, reward, date, status, server);
            
        } catch (Exception e) {
            logger.warn("Error parsing HSR coupon row: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractCode(Element codeCell) {
        // First try to find code in <b> or <code> tags
        Elements boldElements = codeCell.select("b, code");
        if (!boldElements.isEmpty()) {
            String code = boldElements.first().text().trim();
            if (isValidCode(code)) {
                return code;
            }
        }
        
        // Try to find code in links
        Elements links = codeCell.select("a");
        for (Element link : links) {
            String code = link.text().trim();
            if (isValidCode(code)) {
                return code;
            }
        }
        
        // Fallback to cell text
        String cellText = codeCell.text().trim();
        
        // Look for patterns like "HBKKDH9FR3NX" in the text
        Pattern codePattern = Pattern.compile("([A-Z0-9]{8,})");
        Matcher matcher = codePattern.matcher(cellText);
        
        if (matcher.find()) {
            String code = matcher.group(1);
            if (isValidCode(code)) {
                return code;
            }
        }
        
        return null;
    }
    
    private boolean isValidCode(String code) {
        return code != null && 
               code.length() >= 4 && 
               code.matches("[A-Z0-9]+") &&
               !code.equals("CODE"); // Exclude header text
    }
    
    private String extractRewards(Element rewardCell) {
        StringBuilder rewards = new StringBuilder();
        
        // Extract text from reward items
        Elements items = rewardCell.select(".item-text");
        
        if (items.isEmpty()) {
            // Fallback to plain text if no structured items found
            return cleanRewardText(rewardCell.text());
        }
        
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                rewards.append(", ");
            }
            rewards.append(cleanRewardText(items.get(i).text()));
        }
        
        return rewards.toString();
    }
    
    private String cleanRewardText(String text) {
        return text.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("×", " x");
    }
    
    private String extractDate(String dateStatus) {
        // Look for date patterns in the text
        Pattern datePattern = Pattern.compile("(\\w+\\s+\\d{1,2},?\\s+\\d{4}|\\d{1,2}\\w{2}\\s+\\w+\\s+\\d{4}|\\w+\\s+\\d{1,2}\\w{2}|\\d{1,2}\\w{2}\\s+\\w+)");
        Matcher matcher = datePattern.matcher(dateStatus);
        
        if (matcher.find()) {
            return formatDate(matcher.group(1));
        }
        
        // Fallback patterns
        if (dateStatus.contains("Released:")) {
            String[] parts = dateStatus.split("Released:");
            if (parts.length > 1) {
                return formatDate(parts[1].split("Valid")[0].trim());
            }
        }
        
        return "Unknown";
    }
    
    private String formatDate(String rawDate) {
        // Clean up and format the date
        rawDate = rawDate.trim()
                .replaceAll("Released:\\s*", "")
                .replaceAll("Valid.*", "")
                .replaceAll("\\s+", " ");
        
        // Convert month names to ordinal format if needed
        rawDate = rawDate.replaceAll("October (\\d+), (\\d+)", "$1th October $2");
        rawDate = rawDate.replaceAll("September (\\d+), (\\d+)", "$1th September $2");
        rawDate = rawDate.replaceAll("November (\\d+), (\\d+)", "$1th November $2");
        
        return rawDate;
    }
    
    private String determineStatus(String dateStatus) {
        String lowerStatus = dateStatus.toLowerCase();
        
        if (lowerStatus.contains("expired") || lowerStatus.contains("invalid")) {
            return "Expired";
        } else if (lowerStatus.contains("indefinite")) {
            return "Active (Indefinite)";
        } else {
            return "Active";
        }
    }
    
    private boolean isActiveCode(Elements cells) {
        if (cells.isEmpty()) {
            return false;
        }
        
        String dateStatus = cells.get(cells.size() - 1).text().toLowerCase();
        
        // Only include codes that are not explicitly expired
        return !dateStatus.contains("expired") && 
               !dateStatus.contains("invalid") &&
               !dateStatus.contains("hit max usage");
    }
}