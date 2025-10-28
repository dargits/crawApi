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
public class GenshinImpactScraperService {
    
    private static final Logger logger = LoggerFactory.getLogger(GenshinImpactScraperService.class);
    private static final String GENSHIN_WIKI_URL = "https://genshin-impact.fandom.com/wiki/Promotional_Code";
    private static final int TIMEOUT_MS = 10000;
    
    public List<CouponResponse> getActiveCoupons() {
        List<CouponResponse> coupons = new ArrayList<>();
        
        try {
            logger.info("Fetching Genshin Impact promotional codes from: {}", GENSHIN_WIKI_URL);
            
            Document doc = Jsoup.connect(GENSHIN_WIKI_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_MS)
                    .get();
            
            // Find the Active Codes table
            Elements tables = doc.select("table.wikitable.sortable");
            
            for (Element table : tables) {
                Elements rows = table.select("tbody tr");
                
                for (Element row : rows) {
                    Elements cells = row.select("td");
                    
                    if (cells.size() >= 4) {
                        try {
                            CouponResponse coupon = parseCouponRow(cells);
                            if (coupon != null && isActiveCode(cells.get(3).text())) {
                                coupons.add(coupon);
                                logger.debug("Parsed coupon: {}", coupon);
                            }
                        } catch (Exception e) {
                            logger.warn("Error parsing coupon row: {}", e.getMessage());
                        }
                    }
                }
            }
            
            logger.info("Successfully scraped {} active Genshin Impact coupons", coupons.size());
            
        } catch (IOException e) {
            logger.error("Failed to fetch Genshin Impact coupons: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while scraping Genshin Impact coupons: {}", e.getMessage());
        }
        
        return coupons;
    }
    
    private CouponResponse parseCouponRow(Elements cells) {
        try {
            // Extract code from first cell - look for the actual code in links or bold text
            String code = extractCode(cells.get(0));
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            
            // Extract server from second cell
            String server = cells.get(1).text().trim();
            
            // Extract rewards from third cell
            String reward = extractRewards(cells.get(2));
            
            // Extract date and status from fourth cell
            String dateStatus = cells.get(3).text().trim();
            String date = extractDate(dateStatus);
            String status = determineStatus(dateStatus);
            
            return new CouponResponse(code, reward, date, status, server);
            
        } catch (Exception e) {
            logger.warn("Error parsing coupon row: {}", e.getMessage());
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
        
        // Look for patterns like "EKLP57EFE4G4" in the text
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
        if (dateStatus.contains("Discovered:")) {
            String[] parts = dateStatus.split("Discovered:");
            if (parts.length > 1) {
                return formatDate(parts[1].split("Valid")[0].trim());
            }
        }
        
        return "Unknown";
    }
    
    private String formatDate(String rawDate) {
        // Clean up and format the date
        rawDate = rawDate.trim()
                .replaceAll("Discovered:\\s*", "")
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
    
    private boolean isActiveCode(String dateStatus) {
        String lowerStatus = dateStatus.toLowerCase();
        
        // Only include codes that are not explicitly expired
        return !lowerStatus.contains("expired") && 
               !lowerStatus.contains("invalid") &&
               !lowerStatus.contains("hit max usage");
    }
}