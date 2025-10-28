package com.genshin.couponscraper.controller;

import com.genshin.couponscraper.model.CouponResponse;
import com.genshin.couponscraper.service.GenshinImpactScraperService;
import com.genshin.couponscraper.service.HonkaiStarRailScraperService;
import com.genshin.couponscraper.service.BloxFruitsScraperService;
import com.genshin.couponscraper.service.PlayTogetherScraperService;
import com.genshin.couponscraper.service.FCMobileScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/craw")
@CrossOrigin(origins = "*")
public class CouponController {
    
    private static final Logger logger = LoggerFactory.getLogger(CouponController.class);
    
    @Autowired
    private GenshinImpactScraperService genshinImpactScraperService;
    
    @Autowired
    private HonkaiStarRailScraperService honkaiStarRailScraperService;
    
    @Autowired
    private BloxFruitsScraperService bloxFruitsScraperService;
    
    @Autowired
    private PlayTogetherScraperService playTogetherScraperService;
    
    @Autowired
    private FCMobileScraperService fcMobileScraperService;
    
    @GetMapping("/genshin")
    public ResponseEntity<?> getGenshinCoupons() {
        try {
            logger.info("Received request for Genshin Impact coupons");
            
            List<CouponResponse> coupons = genshinImpactScraperService.getActiveCoupons();
            
            logger.info("Returning {} Genshin Impact coupons", coupons.size());
            return ResponseEntity.ok(coupons);
            
        } catch (Exception e) {
            logger.error("Error fetching Genshin Impact coupons: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch Genshin Impact coupons",
                            "message", e.getMessage()
                    ));
        }
    }
    
    @GetMapping("/honkai-star-rail")
    public ResponseEntity<?> getHonkaiStarRailCoupons() {
        try {
            logger.info("Received request for Honkai Star Rail redemption codes");
            
            List<CouponResponse> coupons = honkaiStarRailScraperService.getActiveCoupons();
            
            logger.info("Returning {} Honkai Star Rail coupons", coupons.size());
            return ResponseEntity.ok(coupons);
            
        } catch (Exception e) {
            logger.error("Error fetching Honkai Star Rail coupons: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch Honkai Star Rail coupons",
                            "message", e.getMessage()
                    ));
        }
    }
    
    @GetMapping("/blox-fruits")
    public ResponseEntity<?> getBloxFruitsCoupons() {
        try {
            logger.info("Received request for Blox Fruits codes");
            
            List<CouponResponse> coupons = bloxFruitsScraperService.scrapeActiveCoupons();
            
            logger.info("Returning {} Blox Fruits codes", coupons.size());
            return ResponseEntity.ok(coupons);
            
        } catch (Exception e) {
            logger.error("Error fetching Blox Fruits codes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch Blox Fruits codes",
                            "message", e.getMessage()
                    ));
        }
    }
    
    @GetMapping("/play-together")
    public ResponseEntity<?> getPlayTogetherCoupons() {
        try {
            logger.info("Received request for Play Together coupon codes");
            
            List<CouponResponse> coupons = playTogetherScraperService.getActiveCoupons();
            
            logger.info("Returning {} Play Together coupons", coupons.size());
            return ResponseEntity.ok(coupons);
            
        } catch (Exception e) {
            logger.error("Error fetching Play Together coupons: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch Play Together coupons",
                            "message", e.getMessage()
                    ));
        }
    }
    
    @GetMapping("/fc-mobile")
    public ResponseEntity<?> getFCMobileCoupons() {
        try {
            logger.info("Received request for FC Mobile redeem codes");
            
            List<CouponResponse> coupons = fcMobileScraperService.getActiveCoupons();
            
            logger.info("Returning {} FC Mobile codes", coupons.size());
            return ResponseEntity.ok(coupons);
            
        } catch (Exception e) {
            logger.error("Error fetching FC Mobile codes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch FC Mobile codes",
                            "message", e.getMessage()
                    ));
        }
    }
    

    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "HoYoverse Games Coupon Scraper",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}