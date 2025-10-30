package com.englishvocab.config;

import com.englishvocab.entity.*;
import com.englishvocab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final DictionaryRepository dictionaryRepository;
    private final TopicsRepository topicsRepository;
    private final VocabRepository vocabRepository;
    private final SensesRepository sensesRepository;
    private final VocabTopicsRepository vocabTopicsRepository;
    private final UserVocabProgressRepository userVocabProgressRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Starting English Vocab System Data Initialization...");
        
        initializeUsers();
        initializeDictionaries();
        initializeTopics();
        initializeVocabulary();
        initializeUserProgress();
        
        log.info("🎉 Data Initialization Complete! System ready for use.");
    }

    private void initializeUsers() {
        log.info("👥 Initializing sample users...");
        
        // Check if users already exist
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping user initialization.");
            return;
        }
        
        // 1. Admin User
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullname("Quản trị viên hệ thống")
                    .email("admin@englishvocab.com")
                    .role(User.Role.ADMIN)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(adminUser);
            log.info("✅ Created Admin user: admin / admin123");
        }
        
        // 2. Teacher User
        if (!userRepository.existsByUsername("teacher")) {
            User teacherUser = User.builder()
                    .username("teacher")
                    .password(passwordEncoder.encode("teacher123"))
                    .fullname("Nguyễn Thị Lan - Giáo viên Tiếng Anh")
                    .email("teacher@englishvocab.com")
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(teacherUser);
            log.info("✅ Created Teacher user: teacher / teacher123");
        }
        
        // 3. Student User
        if (!userRepository.existsByUsername("student")) {
            User studentUser = User.builder()
                    .username("student")
                    .password(passwordEncoder.encode("student123"))
                    .fullname("Trần Văn Minh - Học sinh")
                    .email("student@englishvocab.com")
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(studentUser);
            log.info("✅ Created Student user: student / student123");
        }
        
        // 4. Demo User
        if (!userRepository.existsByUsername("demo")) {
            User demoUser = User.builder()
                    .username("demo")
                    .password(passwordEncoder.encode("demo123"))
                    .fullname("Lê Thị Hương - Tài khoản Demo")
                    .email("demo@example.com")
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(demoUser);
            log.info("✅ Created Demo user: demo / demo123");
        }
        
        // 5. Test User
        if (!userRepository.existsByUsername("test")) {
            User testUser = User.builder()
                    .username("test")
                    .password(passwordEncoder.encode("test123"))
                    .fullname("Phạm Văn Test")
                    .email("test@example.com")
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            userRepository.save(testUser);
            log.info("✅ Created Test user: test / test123");
        }
        
        log.info("👥 User initialization completed. Total users: {}", userRepository.count());
    }

    private void initializeDictionaries() {
        log.info("📚 Initializing sample dictionaries...");
        
        // Check if dictionaries already exist
        if (dictionaryRepository.count() > 0) {
            log.info("Dictionaries already exist, skipping dictionary initialization.");
            return;
        }
        
        // 1. TOEIC Business Dictionary
        Dictionary toeicDict = Dictionary.builder()
                .name("TOEIC Business Vocabulary")
                .code("TOEIC_BUS")
                .publisher("ETS Global")
                .description("Essential business vocabulary for TOEIC preparation")
                .status(Dictionary.Status.ACTIVE)
                .build();
        dictionaryRepository.save(toeicDict);
        log.info("✅ Created TOEIC Business Dictionary");
        
        // 2. IELTS Academic Dictionary
        Dictionary ieltsDict = Dictionary.builder()
                .name("IELTS Academic Vocabulary")
                .code("IELTS_ACA")
                .publisher("Cambridge Assessment")
                .description("Academic vocabulary for IELTS examination")
                .status(Dictionary.Status.ACTIVE)
                .build();
        dictionaryRepository.save(ieltsDict);
        log.info("✅ Created IELTS Academic Dictionary");
        
        // 3. Elementary English Dictionary
        Dictionary elementaryDict = Dictionary.builder()
                .name("Elementary English")
                .code("ELEM_ENG")
                .publisher("Oxford University Press")
                .description("Basic English vocabulary for beginners")
                .status(Dictionary.Status.ACTIVE)
                .build();
        dictionaryRepository.save(elementaryDict);
        log.info("✅ Created Elementary English Dictionary");
        
        log.info("📚 Dictionary initialization completed. Total dictionaries: {}", dictionaryRepository.count());
    }

    private void initializeTopics() {
        log.info("🏷️ Initializing sample topics...");
        
        // Check if topics already exist
        if (topicsRepository.count() > 0) {
            log.info("Topics already exist, skipping topics initialization.");
            return;
        }
        
        List<String[]> topicsData = Arrays.asList(
            new String[]{"Business & Work", "Kinh doanh và công việc - từ vựng văn phòng"},
            new String[]{"Travel & Transportation", "Du lịch và giao thông - từ vựng di chuyển"},
            new String[]{"Daily Life", "Cuộc sống hàng ngày - từ vựng sinh hoạt"},
            new String[]{"Education", "Giáo dục - từ vựng học tập và trường học"},
            new String[]{"Technology", "Công nghệ - từ vựng công nghệ thông tin"},
            new String[]{"Health & Medical", "Sức khỏe y tế - từ vựng chăm sóc sức khỏe"},
            new String[]{"Food & Cooking", "Ẩm thực - từ vựng món ăn và nấu nướng"},
            new String[]{"Entertainment", "Giải trí - từ vựng phim ảnh và giải trí"}
        );
        
        for (String[] topicData : topicsData) {
            Topics topic = Topics.builder()
                    .name(topicData[0])
                    .description(topicData[1])
                    .status(Topics.Status.ACTIVE)
                    .build();
            topicsRepository.save(topic);
            log.info("✅ Created topic: {}", topicData[0]);
        }
        
        log.info("🏷️ Topics initialization completed. Total topics: {}", topicsRepository.count());
    }

    private void initializeVocabulary() {
        log.info("📖 Initializing sample vocabulary...");
        
        // Check if vocabulary already exists
        if (vocabRepository.count() > 0) {
            log.info("Vocabulary already exists, skipping vocabulary initialization.");
            return;
        }
        
        // Get dictionaries and topics for relationships
        List<Dictionary> dictionaries = dictionaryRepository.findAll();
        List<Topics> topics = topicsRepository.findAll();
        
        if (dictionaries.isEmpty() || topics.isEmpty()) {
            log.warn("Dictionaries or Topics not found. Cannot initialize vocabulary.");
            return;
        }
        
        Dictionary toeicDict = dictionaries.stream()
                .filter(d -> "TOEIC_BUS".equals(d.getCode())).findFirst().orElse(null);
        Dictionary ieltsDict = dictionaries.stream()
                .filter(d -> "IELTS_ACA".equals(d.getCode())).findFirst().orElse(null);
        Dictionary elementaryDict = dictionaries.stream()
                .filter(d -> "ELEM_ENG".equals(d.getCode())).findFirst().orElse(null);
        
        // Get specific topics
        Topics businessTopic = topics.stream().filter(t -> t.getName().contains("Business")).findFirst().orElse(null);
        Topics travelTopic = topics.stream().filter(t -> t.getName().contains("Travel")).findFirst().orElse(null);
        Topics dailyTopic = topics.stream().filter(t -> t.getName().contains("Daily")).findFirst().orElse(null);
        Topics educationTopic = topics.stream().filter(t -> t.getName().contains("Education")).findFirst().orElse(null);
        Topics technologyTopic = topics.stream().filter(t -> t.getName().contains("Technology")).findFirst().orElse(null);
        
        // Initialize vocabulary for each dictionary
        if (toeicDict != null) {
            initializeToeicVocabulary(toeicDict, businessTopic, travelTopic);
        }
        
        if (ieltsDict != null) {
            initializeIeltsVocabulary(ieltsDict, educationTopic, technologyTopic);
        }
        
        if (elementaryDict != null) {
            initializeElementaryVocabulary(elementaryDict, dailyTopic);
        }
        
        log.info("📖 Vocabulary initialization completed. Total vocabulary: {}", vocabRepository.count());
    }
    
    private void initializeToeicVocabulary(Dictionary dictionary, Topics businessTopic, Topics travelTopic) {
        log.info("📋 Creating TOEIC vocabulary...");
        
        // TOEIC Business vocabulary
        Object[][] toeicWords = {
            {"meeting", "noun", "/ˈmiːtɪŋ/", "INTERMEDIATE", "cuộc họp", "a gathering of people for discussion or decision-making", businessTopic},
            {"presentation", "noun", "/ˌprezənˈteɪʃən/", "INTERMEDIATE", "bài thuyết trình", "a speech or talk about a particular subject", businessTopic},
            {"schedule", "noun", "/ˈʃedjuːl/", "BEGINNER", "lịch trình", "a plan for carrying out a process or procedure", businessTopic},
            {"budget", "noun", "/ˈbʌdʒɪt/", "INTERMEDIATE", "ngân sách", "an estimate of income and expenditure", businessTopic},
            {"deadline", "noun", "/ˈdedlaɪn/", "INTERMEDIATE", "hạn chót", "the latest time or date by which something should be completed", businessTopic},
            {"conference", "noun", "/ˈkɒnfərəns/", "INTERMEDIATE", "hội nghị", "a formal meeting for discussion", businessTopic},
            {"colleague", "noun", "/ˈkɒliːɡ/", "BEGINNER", "đồng nghiệp", "a person with whom one works", businessTopic},
            {"contract", "noun", "/ˈkɒntrækt/", "INTERMEDIATE", "hợp đồng", "a written or spoken agreement", businessTopic},
            {"customer", "noun", "/ˈkʌstəmər/", "BEGINNER", "khách hàng", "a person who buys goods or services", businessTopic},
            {"invoice", "noun", "/ˈɪnvɔɪs/", "INTERMEDIATE", "hóa đơn", "a list of goods sent or services provided", businessTopic},
            {"reservation", "noun", "/ˌrezəˈveɪʃən/", "INTERMEDIATE", "đặt chỗ", "the action of booking accommodation or travel", travelTopic},
            {"departure", "noun", "/dɪˈpɑːrtʃər/", "INTERMEDIATE", "khởi hành", "the action of leaving", travelTopic},
            {"luggage", "noun", "/ˈlʌɡɪdʒ/", "BEGINNER", "hành lý", "suitcases and bags containing personal belongings", travelTopic}
        };
        
        for (Object[] wordData : toeicWords) {
            Vocab vocab = Vocab.builder()
                    .dictionary(dictionary)
                    .word((String) wordData[0])
                    .pos((String) wordData[1])
                    .ipa((String) wordData[2])
                    .level(Vocab.Level.valueOf((String) wordData[3]))
                    .build();
            Vocab savedVocab = vocabRepository.save(vocab);
            
            // Create sense
            Senses sense = Senses.builder()
                    .vocab(savedVocab)
                    .meaningVi((String) wordData[4])
                    .definition((String) wordData[5])
                    .build();
            sensesRepository.save(sense);
            
            // Link to topic
            if (wordData[6] != null) {
                Topics topic = (Topics) wordData[6];
                VocabTopics vocabTopic = VocabTopics.builder()
                        .vocabId(savedVocab.getVocabId())
                        .topicId(topic.getTopicId())
                        .vocab(savedVocab)
                        .topic(topic)
                        .build();
                vocabTopicsRepository.save(vocabTopic);
            }
        }
        
        log.info("✅ Created {} TOEIC vocabulary words", toeicWords.length);
    }
    
    private void initializeIeltsVocabulary(Dictionary dictionary, Topics educationTopic, Topics technologyTopic) {
        log.info("📋 Creating IELTS vocabulary...");
        
        // IELTS Academic vocabulary
        Object[][] ieltsWords = {
            {"analyze", "verb", "/ˈænəlaɪz/", "ADVANCED", "phân tích", "examine in detail the structure of something", educationTopic},
            {"evaluate", "verb", "/ɪˈvæljueɪt/", "ADVANCED", "đánh giá", "form an idea of the amount or value of something", educationTopic},
            {"significant", "adjective", "/sɪɡˈnɪfɪkənt/", "ADVANCED", "quan trọng", "sufficiently great or important to be worthy of attention", educationTopic},
            {"substantial", "adjective", "/səbˈstænʃəl/", "ADVANCED", "đáng kể", "of considerable importance, size, or worth", educationTopic},
            {"hypothesis", "noun", "/haɪˈpɒθəsɪs/", "ADVANCED", "giả thuyết", "a supposition or proposed explanation", educationTopic},
            {"methodology", "noun", "/ˌmeθəˈdɒlədʒi/", "ADVANCED", "phương pháp luận", "a system of methods used in a particular activity", educationTopic},
            {"phenomenon", "noun", "/fɪˈnɒmɪnən/", "ADVANCED", "hiện tượng", "a fact or situation that is observed to exist", educationTopic},
            {"comprehensive", "adjective", "/ˌkɒmprɪˈhensɪv/", "ADVANCED", "toàn diện", "complete and including everything", educationTopic},
            {"innovation", "noun", "/ˌɪnəˈveɪʃən/", "ADVANCED", "sự đổi mới", "the introduction of new ideas or methods", technologyTopic},
            {"digital", "adjective", "/ˈdɪdʒɪtəl/", "INTERMEDIATE", "kỹ thuật số", "relating to computer technology", technologyTopic},
            {"algorithm", "noun", "/ˈælɡərɪðəm/", "ADVANCED", "thuật toán", "a process or set of rules for calculations", technologyTopic}
        };
        
        for (Object[] wordData : ieltsWords) {
            Vocab vocab = Vocab.builder()
                    .dictionary(dictionary)
                    .word((String) wordData[0])
                    .pos((String) wordData[1])
                    .ipa((String) wordData[2])
                    .level(Vocab.Level.valueOf((String) wordData[3]))
                    .build();
            Vocab savedVocab = vocabRepository.save(vocab);
            
            // Create sense
            Senses sense = Senses.builder()
                    .vocab(savedVocab)
                    .meaningVi((String) wordData[4])
                    .definition((String) wordData[5])
                    .build();
            sensesRepository.save(sense);
            
            // Link to topic
            if (wordData[6] != null) {
                Topics topic = (Topics) wordData[6];
                VocabTopics vocabTopic = VocabTopics.builder()
                        .vocabId(savedVocab.getVocabId())
                        .topicId(topic.getTopicId())
                        .vocab(savedVocab)
                        .topic(topic)
                        .build();
                vocabTopicsRepository.save(vocabTopic);
            }
        }
        
        log.info("✅ Created {} IELTS vocabulary words", ieltsWords.length);
    }
    
    private void initializeElementaryVocabulary(Dictionary dictionary, Topics dailyTopic) {
        log.info("📋 Creating Elementary vocabulary...");
        
        // Elementary vocabulary
        Object[][] elementaryWords = {
            {"family", "noun", "/ˈfæmɪli/", "BEGINNER", "gia đình", "a group consisting of parents and children", dailyTopic},
            {"house", "noun", "/haʊs/", "BEGINNER", "nhà", "a building for human habitation", dailyTopic},
            {"school", "noun", "/skuːl/", "BEGINNER", "trường học", "an institution for educating children", dailyTopic},
            {"color", "noun", "/ˈkʌlər/", "BEGINNER", "màu sắc", "the property possessed by an object of producing different sensations on the eye", dailyTopic},
            {"water", "noun", "/ˈwɔːtər/", "BEGINNER", "nước", "a colorless, transparent, odorless liquid", dailyTopic},
            {"food", "noun", "/fuːd/", "BEGINNER", "thức ăn", "any nutritious substance that people eat", dailyTopic},
            {"book", "noun", "/bʊk/", "BEGINNER", "sách", "a written or printed work consisting of pages", dailyTopic},
            {"friend", "noun", "/frend/", "BEGINNER", "bạn bè", "a person whom one knows and has a bond of mutual affection", dailyTopic},
            {"happy", "adjective", "/ˈhæpi/", "BEGINNER", "vui vẻ", "feeling or showing pleasure or contentment", dailyTopic},
            {"beautiful", "adjective", "/ˈbjuːtɪfəl/", "BEGINNER", "đẹp", "pleasing the senses or mind aesthetically", dailyTopic},
            {"big", "adjective", "/bɪɡ/", "BEGINNER", "to", "of considerable size or extent", dailyTopic},
            {"small", "adjective", "/smɔːl/", "BEGINNER", "nhỏ", "of a size that is less than normal", dailyTopic}
        };
        
        for (Object[] wordData : elementaryWords) {
            Vocab vocab = Vocab.builder()
                    .dictionary(dictionary)
                    .word((String) wordData[0])
                    .pos((String) wordData[1])
                    .ipa((String) wordData[2])
                    .level(Vocab.Level.valueOf((String) wordData[3]))
                    .build();
            Vocab savedVocab = vocabRepository.save(vocab);
            
            // Create sense
            Senses sense = Senses.builder()
                    .vocab(savedVocab)
                    .meaningVi((String) wordData[4])
                    .definition((String) wordData[5])
                    .build();
            sensesRepository.save(sense);
            
            // Link to topic
            if (wordData[6] != null) {
                Topics topic = (Topics) wordData[6];
                VocabTopics vocabTopic = VocabTopics.builder()
                        .vocabId(savedVocab.getVocabId())
                        .topicId(topic.getTopicId())
                        .vocab(savedVocab)
                        .topic(topic)
                        .build();
                vocabTopicsRepository.save(vocabTopic);
            }
        }
        
        log.info("✅ Created {} Elementary vocabulary words", elementaryWords.length);
    }
    
    private void initializeUserProgress() {
        log.info("📊 Initializing sample user progress...");
        
        // Check if progress already exists
        if (userVocabProgressRepository.count() > 0) {
            log.info("User progress already exists, skipping progress initialization.");
            return;
        }
        
        // Get sample user
        User sampleUser = userRepository.findByEmail("student@example.com")
            .orElse(null);
            
        if (sampleUser == null) {
            log.warn("Sample user not found, skipping progress initialization.");
            return;
        }
        
        // Get some vocabulary words
        List<Vocab> vocabWords = vocabRepository.findAll().stream()
            .limit(20)
            .collect(java.util.stream.Collectors.toList());
            
        if (vocabWords.isEmpty()) {
            log.warn("No vocabulary words found, skipping progress initialization.");
            return;
        }
        
        // Create progress records for sample user
        for (int i = 0; i < vocabWords.size(); i++) {
            Vocab vocab = vocabWords.get(i);
            
            UserVocabProgress progress = UserVocabProgress.builder()
                .user(sampleUser)
                .vocab(vocab)
                .box(1) // Start with box 1
                .streak(0) // No consecutive correct answers yet
                .wrongCount(0)
                .status(UserVocabProgress.Status.LEARNING)
                .lastReviewed(java.time.LocalDateTime.now().minusDays(i % 3)) // Some reviewed recently
                .nextReviewAt(java.time.LocalDateTime.now().plusDays(i % 2)) // Some due today/tomorrow
                .build();
                
            userVocabProgressRepository.save(progress);
        }
        
        log.info("✅ Created {} progress records for sample user", vocabWords.size());
    }
}
