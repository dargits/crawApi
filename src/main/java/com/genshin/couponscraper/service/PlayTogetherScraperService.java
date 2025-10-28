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

@Service
public class PlayTogetherScraperService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlayTogetherScraperService.class);
    private static final String PLAY_TOGETHER_WIKI_URL = "https://playtogether.fandom.com/wiki/Coupon_Code";
    private static final int TIMEOUT_MS = 10000;
    
    public List<CouponResponse> getActiveCoupons() {
        List<CouponResponse> coupons = new ArrayList<>();
        
        try {
            logger.info("Fetching Play Together coupon codes from: {}", PLAY_TOGETHER_WIKI_URL);
            
            Document doc = Jsoup.connect(PLAY_TOGETHER_WIKI_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_MS)
                    .get();
            
            // Find the current active codes table (first table after "Current codes" text)
            Elements tables = doc.select("table.article-table");
            
            if (!tables.isEmpty()) {
                Element activeTable = tables.first(); // First table contains current active codes
                Elements rows = activeTable.select("tbody tr");
                
                // Skip header row
                for (int i = 1; i < rows.size(); i++) {
                    Element row = rows.get(i);
                    Elements cells = row.select("td");
                    
                    if (cells.size() >= 3) {
                        try {
                            CouponResponse coupon = parseCouponRow(cells);
                            if (coupon != null) {
                                coupons.add(coupon);
                                logger.debug("Parsed Play Together coupon: {}", coupon);
                            }
                        } catch (Exception e) {
                            logger.warn("Error parsing Play Together coupon row: {}", e.getMessage());
                        }
                    }
                }
            }
            
            logger.info("Successfully scraped {} active Play Together coupons", coupons.size());
            
        } catch (IOException e) {
            logger.error("Failed to fetch Play Together coupons: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while scraping Play Together coupons: {}", e.getMessage());
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
            
            // Extract valid until date from second cell
            String validUntil = cells.get(1).text().trim();
            
            // Extract reward from third cell
            String reward = extractReward(cells.get(2));
            
            // All codes in the current table are active
            String status = "Active";
            String server = "Global"; // Play Together is global
            
            return new CouponResponse(code, reward, validUntil, status, server);
            
        } catch (Exception e) {
            logger.warn("Error parsing Play Together coupon row: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractCode(Element codeCell) {
        // Get text content and clean it
        String code = codeCell.text().trim();
        
        // Remove any extra whitespace or formatting
        code = code.replaceAll("\\s+", "");
        
        // Validate code format (should be alphanumeric)
        if (isValidCode(code)) {
            return code;
        }
        
        return null;
    }
    
    private boolean isValidCode(String code) {
        return code != null && 
               code.length() >= 3 && 
               code.matches("[A-Za-z0-9]+") &&
               !code.equalsIgnoreCase("Coupon Code"); // Exclude header text
    }
    
    private String extractReward(Element rewardCell) {
        // Get the HTML content to preserve line breaks
        String rewardHtml = rewardCell.html();
        
        // Replace <br> tags with commas for better formatting
        String reward = rewardHtml.replaceAll("<br\\s*/?>", ", ");
        
        // Remove any remaining HTML tags
        reward = Jsoup.parse(reward).text();
        
        // Clean up extra spaces and commas
        reward = reward.replaceAll("\\s*,\\s*", ", ")
                      .replaceAll("^,\\s*|\\s*,$", "")
                      .trim();
        
        return reward.isEmpty() ? "Unknown reward" : reward;
    }
}