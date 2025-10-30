package com.englishvocab.controller;

import com.englishvocab.dto.AddToListsRequest;
import com.englishvocab.entity.User;
import com.englishvocab.security.CustomUserPrincipal;
import com.englishvocab.service.UserVocabListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for Vocabulary List Operations
 */
@RestController
@RequestMapping("/api/vocab-lists")
@RequiredArgsConstructor
@Slf4j
public class VocabListApiController {

    private final UserVocabListService userVocabListService;

    /**
     * Add system vocabulary to multiple lists
     */
    @PostMapping("/add-system-vocab")
    public ResponseEntity<Map<String, Object>> addSystemVocabToLists(
            @RequestBody AddToListsRequest request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            log.info("User {} adding vocab {} to lists: {}", 
                    currentUser.getUsername(), request.getVocabId(), request.getListIds());
            
            if (request.getVocabId() == null) {
                response.put("success", false);
                response.put("message", "Vocab ID không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getListIds() == null || request.getListIds().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một danh sách");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Add vocab to lists
            int successCount = userVocabListService.addSystemVocabToLists(
                    currentUser.getId(), 
                    request.getVocabId(), 
                    request.getListIds()
            );
            
            if (successCount == 0) {
                response.put("success", false);
                response.put("message", "Không thể thêm từ vựng vào danh sách nào. Vui lòng kiểm tra lại.");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("message", successCount == request.getListIds().size() 
                ? "Đã thêm từ vựng vào " + successCount + " danh sách" 
                : "Đã thêm từ vựng vào " + successCount + "/" + request.getListIds().size() + " danh sách");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error adding vocab to lists", e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Add multiple system vocabularies to multiple lists
     */
    @PostMapping("/add-vocabs")
    public ResponseEntity<Map<String, Object>> addMultipleVocabsToLists(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            @SuppressWarnings("unchecked")
            java.util.List<Integer> listIds = (java.util.List<Integer>) request.get("listIds");
            @SuppressWarnings("unchecked")
            java.util.List<Integer> vocabIds = (java.util.List<Integer>) request.get("vocabIds");
            
            log.info("User {} adding {} vocabs to {} lists", 
                    currentUser.getUsername(), vocabIds.size(), listIds.size());
            
            if (vocabIds == null || vocabIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "Danh sách từ vựng không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (listIds == null || listIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một danh sách");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Add each vocab to all selected lists
            int totalAdded = 0;
            for (Integer vocabId : vocabIds) {
                int successCount = userVocabListService.addSystemVocabToLists(
                        currentUser.getId(), 
                        vocabId, 
                        listIds
                );
                totalAdded += successCount;
            }
            
            response.put("success", true);
            response.put("message", String.format("Đã thêm %d từ vào %d danh sách", vocabIds.size(), listIds.size()));
            response.put("totalAdded", totalAdded);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error adding vocabs to lists", e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Remove system vocabulary from a list
     */
    @DeleteMapping("/{listId}/system-vocab/{vocabId}")
    public ResponseEntity<Map<String, Object>> removeSystemVocabFromList(
            @PathVariable Integer listId,
            @PathVariable Integer vocabId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            log.info("User {} removing vocab {} from list {}", 
                    currentUser.getUsername(), vocabId, listId);
            
            userVocabListService.removeSystemVocab(listId, vocabId, currentUser);
            
            response.put("success", true);
            response.put("message", "Đã xóa từ vựng khỏi danh sách");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error removing vocab from list", e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Helper method to extract current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User user = new User();
        user.setId(principal.getId());
        user.setUsername(principal.getUsername());
        user.setEmail(principal.getEmail());
        user.setFullname(principal.getFullname());
        
        return user;
    }
}

