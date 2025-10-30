package com.englishvocab.controller.admin;

import com.englishvocab.repository.UserVocabProgressRepository;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.TopicsService;
import com.englishvocab.service.UserService;
import com.englishvocab.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserService userService;
    private final DictionaryService dictionaryService;
    private final VocabularyService vocabularyService;
    private final TopicsService topicsService;
    private final UserVocabProgressRepository userVocabProgressRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        UserService.UserStats userStats = userService.getStatistics();
        DictionaryService.DictionaryStats dictionaryStats = dictionaryService.getStatistics();
        VocabularyService.VocabStats vocabStats = vocabularyService.getStatistics();
        TopicsService.TopicStats topicStats = topicsService.getStatistics();

        long totalProgressRecords = userVocabProgressRepository.count();

        int dictionaryActivePercent = percentage(
            dictionaryStats != null ? dictionaryStats.getActive() : 0,
            dictionaryStats != null ? dictionaryStats.getTotal() : 0
        );

        model.addAttribute("pageTitle", "Bảng điều khiển");
        model.addAttribute("activeSection", "dashboard");
        model.addAttribute("userStats", userStats);
        model.addAttribute("dictionaryStats", dictionaryStats);
        model.addAttribute("vocabStats", vocabStats);
        model.addAttribute("topicStats", topicStats);
        model.addAttribute("totalProgressRecords", totalProgressRecords);
        model.addAttribute("dictionaryActivePercent", dictionaryActivePercent);
        model.addAttribute("recentUsers", userService.getRecentUsers(5));
        model.addAttribute("topTopics", topicsService.getTopicsWithVocabCount(5));

        return "admin/dashboard";
    }

    private int percentage(long portion, long total) {
        if (total <= 0) {
            return 0;
        }
        double ratio = (double) portion * 100.0 / (double) total;
        return (int) Math.round(ratio);
    }
}
