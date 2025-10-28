package com.genshin.couponscraper.controller;

import com.genshin.couponscraper.model.CouponResponse;
import com.genshin.couponscraper.service.BloxFruitsScraperService;
import com.genshin.couponscraper.service.GenshinImpactScraperService;
import com.genshin.couponscraper.service.HonkaiStarRailScraperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CouponControllerTest {
    
    @Autowired
    private GenshinImpactScraperService genshinImpactScraperService;
    
    @Autowired
    private HonkaiStarRailScraperService honkaiStarRailScraperService;
    
    @Autowired
    private BloxFruitsScraperService bloxFruitsScraperService;
    
    @Test
    void testGenshinCoupons() {
        System.out.println("=== Testing Genshin Impact Coupons ===");
        List<CouponResponse> coupons = genshinImpactScraperService.getActiveCoupons();
        
        assertNotNull(coupons);
        System.out.println("Found " + coupons.size() + " Genshin Impact coupons:");
        
        for (CouponResponse coupon : coupons) {
            assertNotNull(coupon.getCode());
            assertFalse(coupon.getCode().trim().isEmpty());
            
            System.out.println("Code: " + coupon.getCode());
            System.out.println("Reward: " + coupon.getReward());
            System.out.println("Date: " + coupon.getDate());
            System.out.println("Status: " + coupon.getStatus());
            System.out.println("Server: " + coupon.getServer());
            System.out.println("---");
        }
    }
    
    @Test
    void testHonkaiStarRailCoupons() {
        System.out.println("=== Testing Honkai Star Rail Coupons ===");
        List<CouponResponse> coupons = honkaiStarRailScraperService.getActiveCoupons();
        
        assertNotNull(coupons);
        System.out.println("Found " + coupons.size() + " Honkai Star Rail coupons:");
        
        for (CouponResponse coupon : coupons) {
            assertNotNull(coupon.getCode());
            assertFalse(coupon.getCode().trim().isEmpty());
            
            System.out.println("Code: " + coupon.getCode());
            System.out.println("Reward: " + coupon.getReward());
            System.out.println("Date: " + coupon.getDate());
            System.out.println("Status: " + coupon.getStatus());
            System.out.println("Server: " + coupon.getServer());
            System.out.println("---");
        }
    }
    
    @Test
    void testBloxFruitsCoupons() {
        System.out.println("=== Testing Blox Fruits Codes ===");
        try {
            List<CouponResponse> coupons = bloxFruitsScraperService.scrapeActiveCoupons();
            
            assertNotNull(coupons);
            System.out.println("Found " + coupons.size() + " Blox Fruits codes:");
            
            for (CouponResponse coupon : coupons) {
                assertNotNull(coupon.getCode());
                assertFalse(coupon.getCode().trim().isEmpty());
                
                System.out.println("Code: " + coupon.getCode());
                System.out.println("Reward: " + coupon.getReward());
                System.out.println("Date: " + coupon.getDate());
                System.out.println("Status: " + coupon.getStatus());
                System.out.println("Server: " + coupon.getServer());
                System.out.println("---");
            }
        } catch (Exception e) {
            System.err.println("Error testing Blox Fruits codes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}