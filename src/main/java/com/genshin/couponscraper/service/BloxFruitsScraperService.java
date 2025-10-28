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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BloxFruitsScraperService {

    private static final Logger logger = LoggerFactory.getLogger(BloxFruitsScraperService.class);
    private static final String WIKI_URL = "https://blox-fruits.fandom.com/wiki/Codes";

    public List<CouponResponse> scrapeActiveCoupons() throws IOException {
        logger.info("Starting to scrape Blox Fruits codes from: {}", WIKI_URL);
        
        Document doc = Jsoup.connect(WIKI_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        List<CouponResponse> activeCoupons = new ArrayList<>();
        
        // Target specifically the Working Codes table with id="tpt-1"
        Element workingCodesTable = doc.select("table#tpt-1").first();
        
        if (workingCodesTable != null) {
            logger.info("Found Working Codes table");
            
            // Get all rows from tbody
            Elements rows = workingCodesTable.select("tbody tr");
            logger.info("Found {} rows in Working Codes table", rows.size());
            
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Element row = rows.get(rowIndex);
                
                try {
                    CouponResponse coupon = parseWorkingCodeRow(row);
                    if (coupon != null) {
                        activeCoupons.add(coupon);
                        logger.info("Added working Blox Fruits code: {}", coupon.getCode());
                    }
                } catch (Exception e) {
                    logger.warn("Error parsing working code row {}: {}", rowIndex, e.getMessage());
                }
            }
        } else {
            logger.warn("Working Codes table not found, falling back to general table search");
            
            // Fallback: search for tables with progress tracking class
            Elements tables = doc.select("table.table-progress-tracking");
            logger.info("Found {} progress tracking tables", tables.size());
            
            for (Element table : tables) {
                Elements rows = table.select("tbody tr");
                
                for (Element row : rows) {
                    try {
                        CouponResponse coupon = parseWorkingCodeRow(row);
                        if (coupon != null) {
                            activeCoupons.add(coupon);
                            logger.info("Added Blox Fruits code from fallback: {}", coupon.getCode());
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing fallback row: {}", e.getMessage());
                    }
                }
            }
        }
        
        logger.info("Successfully scraped {} working Blox Fruits codes", activeCoupons.size());
        return activeCoupons;
    }

    private CouponResponse parseWorkingCodeRow(Element row) {
        Elements cells = row.select("td");
        if (cells.size() < 4) {
            return null; // Working codes table has 4 columns: [Checkbox] | Code | Reward | Release Date
        }

        CouponResponse coupon = new CouponResponse();
        
        // Working Codes table structure: [Checkbox] | Code | Reward | Release Date
        int codeColumnIndex = 1; // Code is in second column
        int rewardColumnIndex = 2; // Reward is in third column  
        int dateColumnIndex = 3; // Date is in fourth column
        
        // Parse code from <code> tag
        Element codeCell = cells.get(codeColumnIndex);
        String code = extractCodeFromCodeTag(codeCell);
        if (code == null || code.isEmpty()) {
            logger.debug("No valid code found in cell: {}", codeCell.text());
            return null;
        }
        coupon.setCode(code);

        // Parse reward - handle complex reward structure with icons
        Element rewardCell = cells.get(rewardColumnIndex);
        String reward = parseRewardCell(rewardCell);
        if (reward.isEmpty()) {
            return null;
        }
        coupon.setReward(reward);

        // Parse date
        Element dateCell = cells.get(dateColumnIndex);
        String dateText = dateCell.text().trim();
        String date = formatDate(dateText);
        coupon.setDate(date);

        // All codes in Working Codes table are active
        coupon.setStatus("Active");
        coupon.setServer("Global");
        coupon.setRaw(null);
        
        logger.debug("Successfully parsed working Blox Fruits code: {} with reward: {}", 
                    coupon.getCode(), coupon.getReward());
        
        return coupon;
    }

    private String extractCodeFromCodeTag(Element codeCell) {
        // Look for <code> tag specifically
        Elements codeElements = codeCell.select("code");
        if (!codeElements.isEmpty()) {
            String code = codeElements.first().text().trim();
            if (!code.isEmpty() && code.length() >= 3) {
                return code;
            }
        }
        
        // Fallback to cell text
        String text = codeCell.text().trim();
        if (!text.isEmpty() && text.length() >= 3 && !text.toLowerCase().contains("code")) {
            return text;
        }
        
        return null;
    }

    private String parseRewardCell(Element rewardCell) {
        String rewardText = "";
        
        // Check if reward contains money icon and amount
        Elements moneySpans = rewardCell.select("span.color-currency\\(Money\\)");
        if (!moneySpans.isEmpty()) {
            // Extract money amount
            String moneyText = moneySpans.first().text().trim();
            // Remove the $ symbol and get just the number
            String amount = moneyText.replaceAll("[^0-9]", "");
            if (!amount.isEmpty()) {
                rewardText = amount + " Money";
            }
        } else {
            // Get plain text reward
            rewardText = rewardCell.text().trim();
        }
        
        // Clean up the reward text
        if (rewardText.isEmpty() || rewardText.toLowerCase().contains("reward")) {
            return "";
        }
        
        return cleanRewardText(rewardText);
    }





    private String cleanRewardText(String reward) {
        if (reward == null || reward.isEmpty()) {
            return "Unknown rewards";
        }
        
        // Clean up common wiki formatting
        reward = reward.replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1"); // Remove wiki links
        reward = reward.replaceAll("\\{\\{[^}]+\\}\\}", ""); // Remove templates
        reward = reward.trim();
        
        if (reward.isEmpty()) {
            return "Unknown rewards";
        }
        
        return reward;
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.toLowerCase().contains("date")) {
            return "Unknown";
        }
        
        dateStr = dateStr.trim();
        
        // Handle empty date cells
        if (dateStr.isEmpty()) {
            return "Unknown";
        }
        
        // Handle various date formats
        try {
            // Try parsing "September 3, 2025" format (most common in Blox Fruits)
            if (dateStr.matches("\\w+ \\d{1,2}, \\d{4}")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return formatDateForDisplay(date);
            } else if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                // MM/dd/yyyy format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return formatDateForDisplay(date);
            } else if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                // yyyy-MM-dd format
                LocalDate date = LocalDate.parse(dateStr);
                return formatDateForDisplay(date);
            }
        } catch (DateTimeParseException e) {
            logger.debug("Could not parse date: {}", dateStr);
        }
        
        // If we can't parse it but it's not empty, return as is
        return dateStr.isEmpty() ? "Unknown" : dateStr;
    }

    private String formatDateForDisplay(LocalDate date) {
        int day = date.getDayOfMonth();
        String month = date.getMonth().toString();
        month = month.charAt(0) + month.substring(1).toLowerCase();
        
        return addOrdinalSuffix(String.valueOf(day)) + " " + month;
    }

    private String addOrdinalSuffix(String day) {
        int dayNum = Integer.parseInt(day);
        String suffix;
        
        if (dayNum >= 11 && dayNum <= 13) {
            suffix = "th";
        } else {
            switch (dayNum % 10) {
                case 1: suffix = "st"; break;
                case 2: suffix = "nd"; break;
                case 3: suffix = "rd"; break;
                default: suffix = "th"; break;
            }
        }
        
        return day + suffix;
    }

    private boolean isActiveCoupon(CouponResponse coupon) {
        if (coupon == null || coupon.getCode() == null || coupon.getReward() == null) {
            return false;
        }
        
        String code = coupon.getCode().toLowerCase();
        String reward = coupon.getReward().toLowerCase();
        
        // Skip if code looks like a header or placeholder
        if (code.contains("code") || code.length() < 3) {
            return false;
        }
        
        // Skip if reward looks like a header or is empty
        if (reward.contains("reward") || reward.isEmpty()) {
            return false;
        }
        
        // All codes from Working Codes table are active
        return true;
    }
}