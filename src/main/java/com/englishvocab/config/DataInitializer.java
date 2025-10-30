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
import java.util.stream.Collectors;

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
    private final UserVocabListRepository userVocabListRepository;
    private final DictVocabListRepository dictVocabListRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Starting English Vocab System Data Initialization...");
        
        initializeUsers();
        initializeDictionaries();
        initializeTopics();
        initializeVocabulary();
        initializeUserVocabLists();
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
        Topics healthTopic = topics.stream().filter(t -> t.getName().contains("Health")).findFirst().orElse(null);
        Topics foodTopic = topics.stream().filter(t -> t.getName().contains("Food")).findFirst().orElse(null);
        Topics entertainmentTopic = topics.stream().filter(t -> t.getName().contains("Entertainment")).findFirst().orElse(null);
        
        // Initialize vocabulary for each dictionary
        if (toeicDict != null) {
            initializeToeicVocabulary(toeicDict, businessTopic, travelTopic);
        }
        
        if (ieltsDict != null) {
            initializeIeltsVocabulary(ieltsDict, educationTopic, technologyTopic);
        }
        
        if (elementaryDict != null) {
            initializeElementaryVocabulary(elementaryDict, dailyTopic, foodTopic, healthTopic, entertainmentTopic);
        }
        
        log.info("📖 Vocabulary initialization completed. Total vocabulary: {}", vocabRepository.count());
    }
    
    private void initializeToeicVocabulary(Dictionary dictionary, Topics businessTopic, Topics travelTopic) {
        log.info("📋 Creating TOEIC vocabulary...");
        
        // TOEIC Business vocabulary
        Object[][] toeicWords = {
            {"meeting", "noun", "/ˈmiːtɪŋ/", "INTERMEDIATE", "cuộc họp", "a gathering of people for discussion or decision-making", businessTopic},
            {"presentation", "noun", "/ˌprezənˈteɪʃən/", "INTERMEDIATE", "bài thuyết trình", "a talk that gives information about a subject", businessTopic},
            {"schedule", "noun", "/ˈskedʒuːl/", "BEGINNER", "lịch trình", "a plan that shows when work or events will happen", businessTopic},
            {"budget", "noun", "/ˈbʌdʒɪt/", "INTERMEDIATE", "ngân sách", "an estimate of income and spending for a period", businessTopic},
            {"deadline", "noun", "/ˈdedlaɪn/", "INTERMEDIATE", "hạn chót", "the latest time by which something must be completed", businessTopic},
            {"conference", "noun", "/ˈkɒnfərəns/", "INTERMEDIATE", "hội nghị", "a large formal meeting for discussion", businessTopic},
            {"colleague", "noun", "/ˈkɒliːɡ/", "BEGINNER", "đồng nghiệp", "a person who works with you", businessTopic},
            {"contract", "noun", "/ˈkɒntrækt/", "INTERMEDIATE", "hợp đồng", "a written agreement that is legally binding", businessTopic},
            {"customer", "noun", "/ˈkʌstəmər/", "BEGINNER", "khách hàng", "a person who buys goods or services", businessTopic},
            {"invoice", "noun", "/ˈɪnvɔɪs/", "INTERMEDIATE", "hóa đơn", "a document listing goods supplied and their cost", businessTopic},
            {"reservation", "noun", "/ˌrezərˈveɪʃən/", "INTERMEDIATE", "đặt chỗ", "the act of booking a seat or room in advance", travelTopic},
            {"departure", "noun", "/dɪˈpɑːrtʃər/", "INTERMEDIATE", "khởi hành", "the act of leaving a place", travelTopic},
            {"luggage", "noun", "/ˈlʌɡɪdʒ/", "BEGINNER", "hành lý", "suitcases and bags for a journey", travelTopic},
            {"proposal", "noun", "/prəˈpoʊzəl/", "INTERMEDIATE", "đề xuất", "a plan or suggestion put forward for consideration", businessTopic},
            {"supervisor", "noun", "/ˈsuːpərvaɪzər/", "INTERMEDIATE", "người giám sát", "a person who manages employees or activities", businessTopic},
            {"negotiation", "noun", "/nɪˌɡoʊʃiˈeɪʃən/", "ADVANCED", "đàm phán", "discussion aimed at reaching an agreement", businessTopic},
            {"shipment", "noun", "/ˈʃɪpmənt/", "INTERMEDIATE", "lô hàng", "a quantity of goods sent together", businessTopic},
            {"supplier", "noun", "/səˈplaɪər/", "INTERMEDIATE", "nhà cung cấp", "a company that provides goods or services", businessTopic},
            {"revenue", "noun", "/ˈrevənuː/", "ADVANCED", "doanh thu", "income from sales of goods or services", businessTopic},
            {"expense", "noun", "/ɪkˈspens/", "INTERMEDIATE", "chi phí", "money spent in order to do something", businessTopic},
            {"quarter", "noun", "/ˈkwɔːrtər/", "INTERMEDIATE", "quý", "a three-month period in a financial year", businessTopic},
            {"merger", "noun", "/ˈmɜːrdʒər/", "ADVANCED", "sáp nhập", "the combination of two companies into one", businessTopic},
            {"partnership", "noun", "/ˈpɑːrtnərʃɪp/", "INTERMEDIATE", "quan hệ đối tác", "a cooperative relationship between companies", businessTopic},
            {"client", "noun", "/ˈklaɪənt/", "BEGINNER", "khách hàng", "a person or company that receives professional services", businessTopic},
            {"agenda", "noun", "/əˈdʒendə/", "INTERMEDIATE", "chương trình họp", "a list of matters to be discussed", businessTopic},
            {"feedback", "noun", "/ˈfiːdbæk/", "INTERMEDIATE", "phản hồi", "information about performance used for improvement", businessTopic},
            {"payroll", "noun", "/ˈpeɪroʊl/", "INTERMEDIATE", "bảng lương", "a list of employees and the wages they are paid", businessTopic},
            {"promotion", "noun", "/prəˈmoʊʃən/", "INTERMEDIATE", "thăng chức", "advancement to a higher position", businessTopic},
            {"recruit", "verb", "/rɪˈkruːt/", "INTERMEDIATE", "tuyển dụng", "to find and hire new employees", businessTopic},
            {"resign", "verb", "/rɪˈzaɪn/", "INTERMEDIATE", "từ chức", "to leave a job voluntarily", businessTopic},
            {"commute", "verb", "/kəˈmjuːt/", "BEGINNER", "đi làm hằng ngày", "to travel regularly between home and work", travelTopic},
            {"itinerary", "noun", "/aɪˈtɪnəreri/", "INTERMEDIATE", "lịch trình chuyến đi", "a detailed plan for a journey", travelTopic},
            {"terminal", "noun", "/ˈtɜːrmɪnəl/", "INTERMEDIATE", "nhà ga", "a building where passengers begin or end journeys", travelTopic},
            {"boarding pass", "noun", "/ˈbɔːrdɪŋ pæs/", "BEGINNER", "thẻ lên máy bay", "a document allowing a passenger to board a plane", travelTopic},
            {"delay", "noun", "/dɪˈleɪ/", "BEGINNER", "sự trì hoãn", "a period when something happens later than planned", travelTopic},
            {"upgrade", "verb", "/ʌpˈɡreɪd/", "INTERMEDIATE", "nâng cấp", "to improve the class or quality of a service", travelTopic},
            {"currency", "noun", "/ˈkɜːrənsi/", "BEGINNER", "tiền tệ", "the money used in a particular country", travelTopic},
            {"customs", "noun", "/ˈkʌstəmz/", "INTERMEDIATE", "hải quan", "the government office that controls goods entering a country", travelTopic},
            {"franchise", "noun", "/ˈfrænchaɪz/", "ADVANCED", "nhượng quyền", "permission to use another company's brand and system", businessTopic},
            {"portfolio", "noun", "/pɔːrtˈfoʊlioʊ/", "ADVANCED", "danh mục đầu tư", "a collection of investments or projects", businessTopic},
            {"warehouse", "noun", "/ˈwerhaʊs/", "INTERMEDIATE", "nhà kho", "a large building for storing goods", businessTopic},
            {"logistics", "noun", "/ləˈdʒɪstɪks/", "ADVANCED", "hậu cần", "the detailed coordination of complex operations", businessTopic},
            {"productivity", "noun", "/ˌproʊdʌkˈtɪvəti/", "ADVANCED", "năng suất", "the rate at which work is produced", businessTopic},
            {"benchmark", "noun", "/ˈbentʃmɑːrk/", "ADVANCED", "chuẩn so sánh", "a standard against which others are measured", businessTopic},
            {"outsource", "verb", "/ˈaʊtsɔːrs/", "ADVANCED", "thuê ngoài", "to obtain goods or services from an external supplier", businessTopic},
            {"headquarters", "noun", "/ˈhedˌkwɔːrtərz/", "INTERMEDIATE", "trụ sở chính", "the main office of an organization", businessTopic},
            {"internship", "noun", "/ˈɪntɜːrnʃɪp/", "INTERMEDIATE", "kỳ thực tập", "a period of work experience for students", businessTopic},
            {"seminar", "noun", "/ˈsemɪnɑːr/", "INTERMEDIATE", "hội thảo", "a meeting for training or discussion", businessTopic},
            {"turnover", "noun", "/ˈtɜːrnoʊvər/", "ADVANCED", "tỷ lệ thay nhân sự", "the rate at which employees leave a company", businessTopic},
            {"workforce", "noun", "/ˈwɜːrkfɔːrs/", "INTERMEDIATE", "lực lượng lao động", "all the people working in an organization", businessTopic}
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
            {"analyze", "verb", "/ˈænəlaɪz/", "ADVANCED", "phân tích", "to examine something in detail to understand it", educationTopic},
            {"evaluate", "verb", "/ɪˈvæljueɪt/", "ADVANCED", "đánh giá", "to judge the value or quality of something", educationTopic},
            {"significant", "adjective", "/sɪɡˈnɪfɪkənt/", "ADVANCED", "quan trọng", "large enough to be noticed or have an effect", educationTopic},
            {"substantial", "adjective", "/səbˈstænʃəl/", "ADVANCED", "đáng kể", "considerable in importance, size, or value", educationTopic},
            {"hypothesis", "noun", "/haɪˈpɒθəsɪs/", "ADVANCED", "giả thuyết", "a suggested explanation for an event", educationTopic},
            {"methodology", "noun", "/ˌmeθəˈdɒlədʒi/", "ADVANCED", "phương pháp luận", "a system of methods used in a study", educationTopic},
            {"phenomenon", "noun", "/fɪˈnɒmɪnən/", "ADVANCED", "hiện tượng", "an event or situation that can be observed", educationTopic},
            {"comprehensive", "adjective", "/ˌkɒmprɪˈhensɪv/", "ADVANCED", "toàn diện", "including everything that is necessary", educationTopic},
            {"innovation", "noun", "/ˌɪnəˈveɪʃən/", "ADVANCED", "sự đổi mới", "the introduction of new ideas or methods", technologyTopic},
            {"digital", "adjective", "/ˈdɪdʒɪtəl/", "INTERMEDIATE", "kỹ thuật số", "relating to computer technology", technologyTopic},
            {"algorithm", "noun", "/ˈælɡərɪðəm/", "ADVANCED", "thuật toán", "a set of rules for solving a problem", technologyTopic},
            {"curriculum", "noun", "/kəˈrɪkjələm/", "ADVANCED", "chương trình học", "the subjects studied in a course", educationTopic},
            {"literacy", "noun", "/ˈlɪtərəsi/", "INTERMEDIATE", "khả năng đọc viết", "the ability to read and write", educationTopic},
            {"thesis", "noun", "/ˈθiːsɪs/", "ADVANCED", "luận văn", "a long piece of writing on a particular subject", educationTopic},
            {"citation", "noun", "/saɪˈteɪʃən/", "ADVANCED", "trích dẫn", "a reference to a published or unpublished source", educationTopic},
            {"discipline", "noun", "/ˈdɪsəplɪn/", "INTERMEDIATE", "ngành học", "an area of study or knowledge", educationTopic},
            {"assignment", "noun", "/əˈsaɪnmənt/", "INTERMEDIATE", "bài tập", "a task given as part of a course of study", educationTopic},
            {"lecture", "noun", "/ˈlektʃər/", "INTERMEDIATE", "bài giảng", "a talk given to students on a subject", educationTopic},
            {"campus", "noun", "/ˈkæmpəs/", "BEGINNER", "khuôn viên trường", "the grounds of a university or college", educationTopic},
            {"scholarship", "noun", "/ˈskɑːlərʃɪp/", "INTERMEDIATE", "học bổng", "money given to a student to support education", educationTopic},
            {"enrollment", "noun", "/ɪnˈroʊlmənt/", "INTERMEDIATE", "đăng ký học", "the act of officially joining a course", educationTopic},
            {"tutorial", "noun", "/tuːˈtɔːriəl/", "INTERMEDIATE", "buổi phụ đạo", "a small group class with a tutor", educationTopic},
            {"laboratory", "noun", "/ˈlæbrətɔːri/", "INTERMEDIATE", "phòng thí nghiệm", "a room used for scientific work", educationTopic},
            {"plagiarism", "noun", "/ˈpleɪdʒərɪzəm/", "ADVANCED", "đạo văn", "using someone else's work without credit", educationTopic},
            {"credential", "noun", "/krɪˈdenʃəl/", "ADVANCED", "chứng chỉ", "a qualification proving someone's ability", educationTopic},
            {"proficiency", "noun", "/prəˈfɪʃənsi/", "ADVANCED", "sự thành thạo", "a high degree of skill or ability", educationTopic},
            {"assessment", "noun", "/əˈsesmənt/", "INTERMEDIATE", "đánh giá", "the process of judging a student's work", educationTopic},
            {"syllabus", "noun", "/ˈsɪləbəs/", "INTERMEDIATE", "giáo trình", "an outline of the subjects to be taught", educationTopic},
            {"framework", "noun", "/ˈfreɪmwɜːrk/", "ADVANCED", "khung chương trình", "a basic structure that supports a system", educationTopic},
            {"paradigm", "noun", "/ˈpærədaɪm/", "ADVANCED", "mô hình", "a typical example or pattern of something", educationTopic},
            {"empirical", "adjective", "/ɪmˈpɪrɪkəl/", "ADVANCED", "thực nghiệm", "based on observation or experience", educationTopic},
            {"qualitative", "adjective", "/ˈkwɒlɪtətɪv/", "ADVANCED", "định tính", "relating to qualities rather than numbers", educationTopic},
            {"quantitative", "adjective", "/ˈkwɒntɪtətɪv/", "ADVANCED", "định lượng", "relating to quantity or numbers", educationTopic},
            {"inference", "noun", "/ˈɪnfərəns/", "ADVANCED", "sự suy luận", "a conclusion drawn from evidence", educationTopic},
            {"deduction", "noun", "/dɪˈdʌkʃən/", "ADVANCED", "sự suy diễn", "the process of reasoning from general to specific", educationTopic},
            {"variable", "noun", "/ˈveriəbl/", "INTERMEDIATE", "biến số", "a factor that can change in an experiment", educationTopic},
            {"statistics", "noun", "/stəˈtɪstɪks/", "INTERMEDIATE", "thống kê", "the science of collecting and analyzing data", educationTopic},
            {"simulate", "verb", "/ˈsɪmjəleɪt/", "ADVANCED", "mô phỏng", "to imitate a process for study or training", technologyTopic},
            {"prototype", "noun", "/ˈproʊtətaɪp/", "ADVANCED", "nguyên mẫu", "an original model on which others are based", technologyTopic},
            {"bandwidth", "noun", "/ˈbændwɪtθ/", "ADVANCED", "băng thông", "the capacity for data transfer", technologyTopic},
            {"cybersecurity", "noun", "/ˌsaɪbərsɪˈkjʊrəti/", "ADVANCED", "an ninh mạng", "measures taken to protect computer systems", technologyTopic},
            {"interface", "noun", "/ˈɪntərfeɪs/", "INTERMEDIATE", "giao diện", "a point where two systems meet and interact", technologyTopic},
            {"automation", "noun", "/ˌɔːtəˈmeɪʃən/", "ADVANCED", "tự động hóa", "the use of machines to do work automatically", technologyTopic},
            {"database", "noun", "/ˈdeɪtəbeɪs/", "INTERMEDIATE", "cơ sở dữ liệu", "an organized set of information stored electronically", technologyTopic},
            {"encryption", "noun", "/ɪnˈkrɪpʃən/", "ADVANCED", "mã hóa", "the process of converting data into code", technologyTopic},
            {"robotics", "noun", "/roʊˈbɒtɪks/", "ADVANCED", "ngành robot", "the science of designing and using robots", technologyTopic},
            {"processor", "noun", "/ˈprɑːsesər/", "INTERMEDIATE", "bộ xử lý", "the part of a computer that performs calculations", technologyTopic},
            {"iteration", "noun", "/ˌɪtəˈreɪʃən/", "ADVANCED", "lặp lại", "the repetition of a process to achieve a result", technologyTopic},
            {"optimization", "noun", "/ˌɒptɪməˈzeɪʃən/", "ADVANCED", "tối ưu hóa", "the act of making something as effective as possible", technologyTopic},
            {"collaboration", "noun", "/kəˌlæbəˈreɪʃən/", "INTERMEDIATE", "hợp tác", "the act of working together with others", educationTopic}
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
    
    private void initializeElementaryVocabulary(Dictionary dictionary, Topics dailyTopic, Topics foodTopic,
                                                Topics healthTopic, Topics entertainmentTopic) {
        log.info("📋 Creating Elementary vocabulary...");
        
        // Elementary vocabulary
        Object[][] elementaryWords = {
            {"family", "noun", "/ˈfæmɪli/", "BEGINNER", "gia đình", "a group of people related to each other", dailyTopic},
            {"house", "noun", "/haʊs/", "BEGINNER", "nhà", "a building where people live", dailyTopic},
            {"school", "noun", "/skuːl/", "BEGINNER", "trường học", "a place where children go to learn", dailyTopic},
            {"color", "noun", "/ˈkʌlər/", "BEGINNER", "màu sắc", "the quality of something you see such as red or blue", dailyTopic},
            {"water", "noun", "/ˈwɔːtər/", "BEGINNER", "nước", "a clear liquid that people drink", dailyTopic},
            {"food", "noun", "/fuːd/", "BEGINNER", "thức ăn", "things that people eat to live", foodTopic != null ? foodTopic : dailyTopic},
            {"book", "noun", "/bʊk/", "BEGINNER", "sách", "pages of writing that are fastened together", dailyTopic},
            {"friend", "noun", "/frend/", "BEGINNER", "bạn bè", "a person you like and spend time with", dailyTopic},
            {"happy", "adjective", "/ˈhæpi/", "BEGINNER", "vui vẻ", "feeling or showing pleasure", dailyTopic},
            {"beautiful", "adjective", "/ˈbjuːtɪfəl/", "BEGINNER", "đẹp", "pleasant to look at", dailyTopic},
            {"big", "adjective", "/bɪɡ/", "BEGINNER", "to lớn", "large in size", dailyTopic},
            {"small", "adjective", "/smɔːl/", "BEGINNER", "nhỏ", "little in size", dailyTopic},
            {"morning", "noun", "/ˈmɔːrnɪŋ/", "BEGINNER", "buổi sáng", "the early part of the day", dailyTopic},
            {"night", "noun", "/naɪt/", "BEGINNER", "ban đêm", "the time when it is dark", dailyTopic},
            {"garden", "noun", "/ˈɡɑːrdən/", "BEGINNER", "vườn", "a piece of land with plants and flowers", dailyTopic},
            {"teacher", "noun", "/ˈtiːtʃər/", "BEGINNER", "giáo viên", "a person who helps students learn", dailyTopic},
            {"student", "noun", "/ˈstuːdnt/", "BEGINNER", "học sinh", "a person who studies at school", dailyTopic},
            {"apple", "noun", "/ˈæpəl/", "BEGINNER", "quả táo", "a round fruit with red or green skin", foodTopic != null ? foodTopic : dailyTopic},
            {"bread", "noun", "/bred/", "BEGINNER", "bánh mì", "a food made from flour and baked in an oven", foodTopic != null ? foodTopic : dailyTopic},
            {"rice", "noun", "/raɪs/", "BEGINNER", "gạo", "small white grains eaten as food", foodTopic != null ? foodTopic : dailyTopic},
            {"soup", "noun", "/suːp/", "BEGINNER", "súp", "a hot liquid food made by boiling meat or vegetables", foodTopic != null ? foodTopic : dailyTopic},
            {"coffee", "noun", "/ˈkɒfi/", "BEGINNER", "cà phê", "a hot drink made from roasted beans", foodTopic != null ? foodTopic : dailyTopic},
            {"tea", "noun", "/tiː/", "BEGINNER", "trà", "a hot drink made by soaking dried leaves in water", foodTopic != null ? foodTopic : dailyTopic},
            {"doctor", "noun", "/ˈdɒktər/", "BEGINNER", "bác sĩ", "a person trained to treat sick people", healthTopic != null ? healthTopic : dailyTopic},
            {"nurse", "noun", "/nɜːrs/", "BEGINNER", "y tá", "a person who cares for people in hospital", healthTopic != null ? healthTopic : dailyTopic},
            {"hospital", "noun", "/ˈhɒspɪtl/", "BEGINNER", "bệnh viện", "a building where sick people get treatment", healthTopic != null ? healthTopic : dailyTopic},
            {"healthy", "adjective", "/ˈhelθi/", "BEGINNER", "khỏe mạnh", "feeling well and not ill", healthTopic != null ? healthTopic : dailyTopic},
            {"exercise", "noun", "/ˈeksərsaɪz/", "BEGINNER", "tập thể dục", "activity that keeps your body strong", healthTopic != null ? healthTopic : dailyTopic},
            {"medicine", "noun", "/ˈmedɪsən/", "BEGINNER", "thuốc", "something you take when you are sick", healthTopic != null ? healthTopic : dailyTopic},
            {"walk", "verb", "/wɔːk/", "BEGINNER", "đi bộ", "to move by putting one foot in front of the other", dailyTopic},
            {"run", "verb", "/rʌn/", "BEGINNER", "chạy", "to move quickly on your feet", dailyTopic},
            {"jump", "verb", "/dʒʌmp/", "BEGINNER", "nhảy", "to push yourself off the ground into the air", dailyTopic},
            {"play", "verb", "/pleɪ/", "BEGINNER", "chơi", "to do something for fun", dailyTopic},
            {"music", "noun", "/ˈmjuːzɪk/", "BEGINNER", "âm nhạc", "sounds made by voices or instruments", entertainmentTopic != null ? entertainmentTopic : dailyTopic},
            {"movie", "noun", "/ˈmuːvi/", "BEGINNER", "bộ phim", "a story shown on a screen", entertainmentTopic != null ? entertainmentTopic : dailyTopic},
            {"song", "noun", "/sɔːŋ/", "BEGINNER", "bài hát", "a short piece of music with words", entertainmentTopic != null ? entertainmentTopic : dailyTopic},
            {"dance", "verb", "/dæns/", "BEGINNER", "nhảy múa", "to move your body to music", entertainmentTopic != null ? entertainmentTopic : dailyTopic},
            {"smile", "verb", "/smaɪl/", "BEGINNER", "mỉm cười", "to make a happy face with your mouth", dailyTopic},
            {"laugh", "verb", "/læf/", "BEGINNER", "cười", "to make sounds that show you are happy", dailyTopic},
            {"sleep", "verb", "/sliːp/", "BEGINNER", "ngủ", "to rest your body with your eyes closed", dailyTopic},
            {"wake", "verb", "/weɪk/", "BEGINNER", "thức dậy", "to stop sleeping", dailyTopic},
            {"clean", "verb", "/kliːn/", "BEGINNER", "dọn sạch", "to make something free from dirt", dailyTopic},
            {"cook", "verb", "/kʊk/", "BEGINNER", "nấu ăn", "to prepare food by heating it", foodTopic != null ? foodTopic : dailyTopic},
            {"bake", "verb", "/beɪk/", "BEGINNER", "nướng", "to cook food like bread or cake in an oven", foodTopic != null ? foodTopic : dailyTopic},
            {"chair", "noun", "/tʃer/", "BEGINNER", "ghế", "a seat for one person", dailyTopic},
            {"table", "noun", "/ˈteɪbəl/", "BEGINNER", "bàn", "a piece of furniture with a flat top", dailyTopic},
            {"window", "noun", "/ˈwɪndoʊ/", "BEGINNER", "cửa sổ", "an opening in a wall to let in light", dailyTopic},
            {"door", "noun", "/dɔːr/", "BEGINNER", "cửa ra vào", "a movable part that opens and closes an entrance", dailyTopic},
            {"street", "noun", "/striːt/", "BEGINNER", "đường phố", "a road in a town or city", dailyTopic},
            {"market", "noun", "/ˈmɑːrkɪt/", "BEGINNER", "chợ", "a place where people buy and sell things", dailyTopic}
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

    private void initializeUserVocabLists() {
        log.info("🗂️ Initializing sample user vocabulary lists...");

        if (userVocabListRepository.count() > 0) {
            log.info("User vocabulary lists already exist, skipping list initialization.");
            return;
        }

        User teacher = userRepository.findByUsername("teacher").orElse(null);
        User student = userRepository.findByUsername("student").orElse(null);

        if (teacher == null || student == null) {
            log.warn("Teacher or student user not found. Cannot initialize vocabulary lists.");
            return;
        }

        Dictionary toeicDict = dictionaryRepository.findByCode("TOEIC_BUS").orElse(null);
        Dictionary elementaryDict = dictionaryRepository.findByCode("ELEM_ENG").orElse(null);

        if (toeicDict == null || elementaryDict == null) {
            log.warn("Required dictionaries not found. Cannot initialize vocabulary lists.");
            return;
        }

        List<Vocab> toeicHighlights = vocabRepository.findByDictionaryOrderByWordAsc(toeicDict)
                .stream()
                .limit(10)
                .collect(Collectors.toList());
        List<Vocab> dailyEssentials = vocabRepository.findByDictionaryOrderByWordAsc(elementaryDict)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        if (toeicHighlights.size() < 10 || dailyEssentials.size() < 10) {
            log.warn("Not enough vocabulary words to populate demo lists.");
            return;
        }

        UserVocabList businessStarter = userVocabListRepository.save(UserVocabList.builder()
                .user(teacher)
                .name("Business Starter Pack")
                .description("10 từ TOEIC quan trọng cho buổi họp đầu tuần")
                .visibility(UserVocabList.Visibility.PUBLIC)
                .status(UserVocabList.Status.ACTIVE)
                .build());
        addWordsToList(businessStarter, toeicHighlights);

        UserVocabList dailyLife = userVocabListRepository.save(UserVocabList.builder()
                .user(student)
                .name("Daily Life Essentials")
                .description("10 từ vựng giao tiếp hằng ngày cho người mới bắt đầu")
                .visibility(UserVocabList.Visibility.PRIVATE)
                .status(UserVocabList.Status.ACTIVE)
                .build());
        addWordsToList(dailyLife, dailyEssentials);

        log.info("🗂️ Created {} user vocabulary lists for demo", userVocabListRepository.count());
    }

    private void addWordsToList(UserVocabList list, List<Vocab> words) {
        for (Vocab vocab : words) {
            DictVocabList entry = DictVocabList.builder()
                    .userVocabList(list)
                    .vocab(vocab)
                    .build();
            dictVocabListRepository.save(entry);
        }
    }

    private void initializeUserProgress() {
        log.info("📊 Initializing sample user progress...");

        // Check if progress already exists
        if (userVocabProgressRepository.count() > 0) {
            log.info("User progress already exists, skipping progress initialization.");
            return;
        }

        List<User> learners = userRepository.findByRole(User.Role.USER);

        if (learners.isEmpty()) {
            log.warn("No learner accounts found, skipping progress initialization.");
            return;
        }

        List<Vocab> allVocabulary = vocabRepository.findAll();

        if (allVocabulary.isEmpty()) {
            log.warn("No vocabulary words found, skipping progress initialization.");
            return;
        }

        int recordsPerUser = Math.min(20, allVocabulary.size());
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int recordsCreated = 0;

        for (int userIndex = 0; userIndex < learners.size(); userIndex++) {
            User learner = learners.get(userIndex);

            for (int i = 0; i < recordsPerUser; i++) {
                Vocab vocab = allVocabulary.get((userIndex * recordsPerUser + i) % allVocabulary.size());
                int baseBox = (i % 5) + 1;
                int wrongCount = (i % 6 == 0) ? 2 : ((i % 4 == 0) ? 1 : 0);
                int box = wrongCount >= 2 ? Math.max(1, baseBox - 1) : baseBox;
                int streak = wrongCount > 0 ? 0 : Math.min(3, i % 4);

                UserVocabProgress.Status status;
                if (wrongCount >= 2) {
                    status = UserVocabProgress.Status.DIFFICULT;
                } else if (box >= 4) {
                    status = UserVocabProgress.Status.MASTERED;
                } else if (box == 3) {
                    status = UserVocabProgress.Status.REVIEWING;
                } else if (box == 2) {
                    status = UserVocabProgress.Status.LEARNING;
                } else {
                    status = wrongCount == 0 ? UserVocabProgress.Status.NEW : UserVocabProgress.Status.LEARNING;
                }

                long nextReviewDays = switch (box) {
                    case 1 -> 1L;
                    case 2 -> 3L;
                    case 3 -> 7L;
                    case 4 -> 14L;
                    case 5 -> 30L;
                    default -> 1L;
                };

                UserVocabProgress progress = UserVocabProgress.builder()
                        .user(learner)
                        .vocab(vocab)
                        .box(box)
                        .streak(streak)
                        .wrongCount(wrongCount)
                        .status(status)
                        .firstLearned(now.minusDays(8 + userIndex).minusDays(i % 5))
                        .lastReviewed(now.minusDays(i % 4))
                        .nextReviewAt(now.plusDays(nextReviewDays))
                        .build();

                userVocabProgressRepository.save(progress);
                recordsCreated++;
            }
        }

        log.info("✅ Created {} progress records for demo users", recordsCreated);
    }
}
