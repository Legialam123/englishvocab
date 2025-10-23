/**
 * PronunciationPlayer.js
 * Complete pronunciation system with Web Speech API
 * 
 * Features:
 * - Play pronunciation with different voices
 * - Speed control (slow/normal/fast)
 * - Accent selection (US/UK/AU)
 * - Fallback handling
 * - Event callbacks
 */

class PronunciationPlayer {
    constructor(options = {}) {
        this.synth = window.speechSynthesis;
        this.voices = [];
        this.currentVoice = null;
        this.defaultRate = options.defaultRate || 0.9;
        this.defaultPitch = options.defaultPitch || 1.0;
        this.defaultVolume = options.defaultVolume || 1.0;
        this.preferredAccent = options.preferredAccent || 'US'; // US, UK, AU
        this.voicesLoaded = false;
        this.voiceLoadPromise = null;
        this.isSpeaking = false; // Track speaking state
        this.lastSpeakTime = 0; // Debounce protection
        
        // Initialize voice loading
        this.initVoiceLoading();
    }
    
    /**
     * Initialize voice loading with promise-based approach
     */
    initVoiceLoading() {
        this.voiceLoadPromise = new Promise((resolve) => {
            // Try immediate load
            this.loadVoices();
            
            if (this.voices.length > 0) {
                this.voicesLoaded = true;
                resolve();
                return;
            }
            
            // Wait for voices to load asynchronously
            if (this.synth.onvoiceschanged !== undefined) {
                this.synth.onvoiceschanged = () => {
                    this.loadVoices();
                    if (this.voices.length > 0) {
                        this.voicesLoaded = true;
                        resolve();
                    }
                };
            }
            
            // Fallback: retry after 100ms
            setTimeout(() => {
                this.loadVoices();
                if (this.voices.length > 0) {
                    this.voicesLoaded = true;
                }
                resolve(); // Resolve anyway to prevent hanging
            }, 100);
        });
    }
    
    /**
     * Load available voices and set default
     */
    loadVoices() {
        const previousCount = this.voices.length;
        this.voices = this.synth.getVoices();
        
        if (this.voices.length === 0) {
            console.warn('⏳ No voices loaded yet, will retry...');
            return;
        }
        
        // Skip logging if voices already loaded (prevent duplicate logs)
        if (previousCount === this.voices.length && this.currentVoice) {
            return;
        }
        
        // Find best voice based on preferred accent
        const accentMap = {
            'US': 'en-US',
            'UK': 'en-GB',
            'AU': 'en-AU'
        };
        
        const preferredLang = accentMap[this.preferredAccent] || 'en-US';
        
        // Try to find preferred voice
        this.currentVoice = this.voices.find(v => v.lang === preferredLang) ||
                           this.voices.find(v => v.lang.startsWith('en')) ||
                           this.voices[0];
        
        console.log(`✅ Loaded ${this.voices.length} voices. Current: ${this.currentVoice?.name || 'default'}`);
    }
    
    /**
     * Main method to speak text
     * @param {string} text - Text to pronounce
     * @param {object} options - Speaking options
     */
    async speak(text, options = {}) {
        if (!text) {
            console.warn('No text provided to speak');
            return;
        }
        
        // Debounce protection (prevent rapid clicks)
        const now = Date.now();
        if (now - this.lastSpeakTime < 300) {
            console.log('⏭️ Debounced: too fast');
            return;
        }
        this.lastSpeakTime = now;
        
        // Check if already speaking
        if (this.isSpeaking) {
            console.log('⏭️ Already speaking, canceling previous...');
            this.stop();
            // Small delay to allow cancellation
            await new Promise(resolve => setTimeout(resolve, 50));
        }
        
        // Wait for voices to load
        await this.voiceLoadPromise;
        
        // Check if voices are available
        if (this.voices.length === 0) {
            console.error('❌ No voices available for speech synthesis');
            if (options.onError) {
                options.onError(new Error('No voices available'));
            }
            return;
        }
        
        const utterance = new SpeechSynthesisUtterance(text);
        
        // Set voice
        utterance.voice = options.voice || this.currentVoice;
        
        // Set parameters
        utterance.rate = options.rate !== undefined ? options.rate : this.defaultRate;
        utterance.pitch = options.pitch !== undefined ? options.pitch : this.defaultPitch;
        utterance.volume = options.volume !== undefined ? options.volume : this.defaultVolume;
        utterance.lang = options.lang || (this.currentVoice?.lang || 'en-US');
        
        // Track speaking state
        this.isSpeaking = true;
        
        // Event handlers
        utterance.onstart = () => {
            console.log(`🔊 Speaking: "${text}"`);
            if (options.onStart) options.onStart();
        };
        
        utterance.onend = () => {
            console.log(`✅ Finished: "${text}"`);
            this.isSpeaking = false;
            if (options.onEnd) options.onEnd();
        };
        
        utterance.onerror = (event) => {
            // Ignore "interrupted" errors (normal when canceling)
            if (event.error === 'interrupted') {
                console.log('⏭️ Speech interrupted (normal)');
                this.isSpeaking = false;
                return;
            }
            
            console.error('❌ Speech error:', event.error);
            this.isSpeaking = false;
            if (options.onError) {
                options.onError(event);
            } else {
                // Default error handling
                this.handleSpeechError(event, text);
            }
        };
        
        utterance.onpause = () => {
            if (options.onPause) options.onPause();
        };
        
        utterance.onresume = () => {
            if (options.onResume) options.onResume();
        };
        
        // Speak
        try {
            this.synth.speak(utterance);
        } catch (error) {
            console.error('❌ Failed to speak:', error);
            this.isSpeaking = false;
            if (options.onError) {
                options.onError(error);
            }
        }
    }
    
    /**
     * Stop current speech
     */
    stop() {
        if (this.synth.speaking) {
            this.synth.cancel();
            this.isSpeaking = false;
        }
    }
    
    /**
     * Check if player is ready (voices loaded)
     * @returns {boolean}
     */
    isReady() {
        return this.voicesLoaded && this.voices.length > 0;
    }
    
    /**
     * Wait for player to be ready
     * @returns {Promise}
     */
    async waitUntilReady() {
        await this.voiceLoadPromise;
        return this.isReady();
    }
    
    /**
     * Pause current speech
     */
    pause() {
        if (this.synth.speaking && !this.synth.paused) {
            this.synth.pause();
        }
    }
    
    /**
     * Resume paused speech
     */
    resume() {
        if (this.synth.paused) {
            this.synth.resume();
        }
    }
    
    /**
     * Get all available English voices
     * @returns {Array} Array of voice objects
     */
    getEnglishVoices() {
        return this.voices.filter(v => v.lang.startsWith('en'));
    }
    
    /**
     * Get voices by accent
     * @param {string} accent - US, UK, AU, etc.
     * @returns {Array} Array of matching voices
     */
    getVoicesByAccent(accent) {
        const accentMap = {
            'US': 'en-US',
            'UK': 'en-GB',
            'AU': 'en-AU',
            'IN': 'en-IN',
            'CA': 'en-CA'
        };
        
        const lang = accentMap[accent];
        if (!lang) return [];
        
        return this.voices.filter(v => v.lang === lang);
    }
    
    /**
     * Set voice by name
     * @param {string} voiceName - Name of the voice
     * @returns {boolean} Success status
     */
    setVoice(voiceName) {
        const voice = this.voices.find(v => v.name === voiceName);
        if (voice) {
            this.currentVoice = voice;
            console.log(`Voice changed to: ${voiceName}`);
            return true;
        }
        console.warn(`Voice not found: ${voiceName}`);
        return false;
    }
    
    /**
     * Set voice by accent
     * @param {string} accent - US, UK, AU, etc.
     * @returns {boolean} Success status
     */
    setAccent(accent) {
        const voices = this.getVoicesByAccent(accent);
        if (voices.length > 0) {
            this.currentVoice = voices[0];
            this.preferredAccent = accent;
            console.log(`Accent changed to: ${accent} (${this.currentVoice.name})`);
            return true;
        }
        console.warn(`No voices found for accent: ${accent}`);
        return false;
    }
    
    /**
     * Speak with specific speed preset
     */
    speakSlow(text, options = {}) {
        this.speak(text, { ...options, rate: 0.6 });
    }
    
    speakNormal(text, options = {}) {
        this.speak(text, { ...options, rate: 0.9 });
    }
    
    speakFast(text, options = {}) {
        this.speak(text, { ...options, rate: 1.2 });
    }
    
    /**
     * Handle speech errors with fallback
     */
    handleSpeechError(event, text) {
        console.error('Speech synthesis error:', event.error);
        
        // Show user-friendly message
        const errorMessages = {
            'audio-busy': 'Thiết bị âm thanh đang bận. Vui lòng thử lại.',
            'audio-hardware': 'Lỗi phần cứng âm thanh.',
            'network': 'Lỗi mạng. Kiểm tra kết nối internet.',
            'synthesis-unavailable': 'Tính năng phát âm không khả dụng.',
            'synthesis-failed': 'Không thể phát âm. Vui lòng thử lại.',
            'language-unavailable': 'Ngôn ngữ không được hỗ trợ.',
            'voice-unavailable': 'Giọng nói không khả dụng.',
            'text-too-long': 'Văn bản quá dài để phát âm.',
            'invalid-argument': 'Tham số không hợp lệ.',
            'not-allowed': 'Trình duyệt không cho phép phát âm tự động. Vui lòng click nút phát âm.',
            'interrupted': 'Phát âm bị gián đoạn.'
        };
        
        const message = errorMessages[event.error] || 'Lỗi không xác định khi phát âm.';
        
        // Show notification (integrate with your notification system)
        this.showNotification('error', message);
        
        // Try fallback with different voice
        if (event.error === 'voice-unavailable') {
            const fallbackVoices = this.getEnglishVoices();
            if (fallbackVoices.length > 1) {
                console.log('Trying fallback voice...');
                this.currentVoice = fallbackVoices[1];
                this.speak(text);
            }
        }
    }
    
    /**
     * Show notification (override this with your app's notification system)
     */
    showNotification(type, message) {
        console.log(`[${type.toUpperCase()}] ${message}`);
        // TODO: Integrate with Bootstrap toast or your notification system
    }
    
    /**
     * Check if speech synthesis is supported
     * @returns {boolean}
     */
    static isSupported() {
        return 'speechSynthesis' in window;
    }
    
    /**
     * Get current speaking status
     * @returns {object} Status object
     */
    getStatus() {
        return {
            speaking: this.synth.speaking,
            paused: this.synth.paused,
            pending: this.synth.pending,
            currentVoice: this.currentVoice?.name,
            currentAccent: this.preferredAccent,
            availableVoices: this.voices.length
        };
    }
}

// ============ Usage Examples (COMMENTED OUT - For reference only) ============

/*
// Example 1: Basic usage
const player = new PronunciationPlayer();
player.speak('Hello, world!');

// Example 2: With options
player.speak('Hello', {
    rate: 0.8,
    pitch: 1.0,
    volume: 1.0,
    onEnd: () => console.log('Finished!'),
    onError: (e) => console.error('Error:', e)
});

// Example 3: Speed presets
player.speakSlow('Difficult word');
player.speakNormal('Normal speed');
player.speakFast('Quick pronunciation');

// Example 4: Accent switching
player.setAccent('UK');
player.speak('Schedule'); // British pronunciation

player.setAccent('US');
player.speak('Schedule'); // American pronunciation

// Example 5: Voice selection
const voices = player.getEnglishVoices();
console.log('Available voices:', voices.map(v => v.name));
player.setVoice('Google US English');

// Example 6: Control playback
player.speak('Long sentence...');
setTimeout(() => player.pause(), 1000);
setTimeout(() => player.resume(), 2000);
setTimeout(() => player.stop(), 3000);
*/

// ============ Vocabulary Card Integration (REFERENCE ONLY) ============

/*
// HTML Example:

<div class="vocab-card" data-word="hello">
    <h3 class="vocab-word">hello</h3>
    <div class="vocab-phonetic">/həˈloʊ/</div>
    
    <div class="pronunciation-controls">
        <button class="btn-play" onclick="playWord(this)">
            <i class="fas fa-volume-up"></i>
        </button>
        
        <select class="accent-selector" onchange="changeAccent(this)">
            <option value="US">🇺🇸 US</option>
            <option value="UK">🇬🇧 UK</option>
            <option value="AU">🇦🇺 AU</option>
        </select>
        
        <div class="btn-group speed-controls">
            <button onclick="playSpeed(this, 0.6)">
                <i class="fas fa-turtle"></i> Chậm
            </button>
            <button onclick="playSpeed(this, 0.9)" class="active">
                <i class="fas fa-walking"></i> Vừa
            </button>
            <button onclick="playSpeed(this, 1.2)">
                <i class="fas fa-running"></i> Nhanh
            </button>
        </div>
    </div>
</div>
*/

// Export for use in other modules (Node.js compatibility)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PronunciationPlayer;
}
