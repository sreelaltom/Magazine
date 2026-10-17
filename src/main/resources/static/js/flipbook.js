/**
 * 3D Page Flip Magazine Viewer
 * Powered by PDF.js and StPageFlip
 */

let currentFlipBook = null;
let audioContext = null;
let soundEnabled = true;

// Set PDF.js worker path
if (window.pdfjsLib) {
    pdfjsLib.GlobalWorkerOptions.workerSrc = '/js/pdf.worker.min.js';
}

/**
 * Synthesizes a realistic paper flip sound using the Web Audio API
 */
function playPageTurnSound() {
    if (!soundEnabled) return;
    try {
        if (!audioContext) {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (audioContext.state === 'suspended') {
            audioContext.resume();
        }

        const bufferSize = audioContext.sampleRate * 0.15; // 150ms
        const buffer = audioContext.createBuffer(1, bufferSize, audioContext.sampleRate);
        const data = buffer.getChannelData(0);

        // Generate filtered noise
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (bufferSize * 0.35));
        }

        const noise = audioContext.createBufferSource();
        noise.buffer = buffer;

        // Bandpass filter to shape paper rustle tone
        const filter = audioContext.createBiquadFilter();
        filter.type = 'bandpass';
        filter.frequency.value = 1400;
        filter.Q.value = 1.2;

        const gain = audioContext.createGain();
        gain.gain.setValueAtTime(0.4, audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.14);

        noise.connect(filter);
        filter.connect(gain);
        gain.connect(audioContext.destination);

        noise.start();
    } catch (e) {
        // Audio policy or unsupported
    }
}

/**
 * Toggle sound effect
 */
function toggleSound() {
    soundEnabled = !soundEnabled;
    const btn = document.getElementById('soundBtn');
    if (btn) {
        btn.innerHTML = soundEnabled ? '🔊 Sound On' : '🔇 Muted';
        btn.classList.toggle('active', soundEnabled);
    }
}

/**
 * Open and render the 3D Animated Flipbook
 */
async function openPdfReader(url, title) {
    const modal = document.getElementById('pdfModal');
    const titleElem = document.getElementById('modalTitle');
    const downloadBtn = document.getElementById('modalDownloadBtn');
    const container = document.getElementById('flipbookContainer');
    const loadingElem = document.getElementById('flipbookLoading');
    const pageIndicator = document.getElementById('pageIndicator');

    if (!modal || !container) return;

    if (titleElem) titleElem.textContent = title || 'Magazine Viewer';
    if (downloadBtn) downloadBtn.href = url.replace('/view/', '/download/');

    modal.classList.add('active');
    document.body.style.overflow = 'hidden';

    // Clear previous flipbook instance
    if (currentFlipBook) {
        try { currentFlipBook.destroy(); } catch (e) {}
        currentFlipBook = null;
    }
    container.innerHTML = '';
    if (loadingElem) loadingElem.style.display = 'flex';
    if (pageIndicator) pageIndicator.textContent = 'Loading...';

    try {
        // Load PDF document
        const loadingTask = pdfjsLib.getDocument(url);
        const pdf = await loadingTask.promise;
        const totalPages = pdf.numPages;

        const bookWrapper = document.createElement('div');
        bookWrapper.className = 'flip-book-wrapper';
        bookWrapper.id = 'flipBookInstance';
        container.appendChild(bookWrapper);

        // Determine viewport sizing for optimal crispness
        const isMobile = window.innerWidth <= 768;
        const baseWidth = isMobile ? Math.min(window.innerWidth - 30, 420) : 480;
        const baseHeight = isMobile ? Math.min(window.innerHeight - 150, 620) : 660;

        // Render each page into canvas
        for (let pageNum = 1; pageNum <= totalPages; pageNum++) {
            const page = await pdf.getPage(pageNum);
            const viewport = page.getViewport({ scale: 1.5 });

            const pageDiv = document.createElement('div');
            pageDiv.className = 'page-container';

            const canvas = document.createElement('canvas');
            canvas.className = 'page-canvas';
            const context = canvas.getContext('2d');
            canvas.width = viewport.width;
            canvas.height = viewport.height;

            const renderContext = {
                canvasContext: context,
                viewport: viewport
            };
            await page.render(renderContext).promise;

            pageDiv.appendChild(canvas);

            // Add subtle page curl hint on cover
            if (pageNum === 1) {
                const hint = document.createElement('div');
                hint.className = 'page-corner-hint';
                hint.title = 'Drag or swipe corner to turn page';
                pageDiv.appendChild(hint);
            }

            bookWrapper.appendChild(pageDiv);
        }

        if (loadingElem) loadingElem.style.display = 'none';

        // Initialize St.PageFlip
        currentFlipBook = new St.PageFlip(bookWrapper, {
            width: baseWidth,
            height: baseHeight,
            size: 'stretch',
            minWidth: 260,
            maxWidth: 800,
            minHeight: 380,
            maxHeight: 1100,
            maxShadowOpacity: 0.6,
            showCover: true,
            mobileScrollSupport: false,
            usePortrait: true,
            startPage: 0,
            flippingTime: 800,
            useMouseEvents: true,
            swipeDistance: 20
        });

        const pages = bookWrapper.querySelectorAll('.page-container');
        currentFlipBook.loadFromHTML(pages);

        // Update page indicator on flip and play sound
        currentFlipBook.on('flip', (e) => {
            const current = e.data + 1;
            if (pageIndicator) {
                pageIndicator.textContent = `Page ${current} of ${totalPages}`;
            }
            playPageTurnSound();
        });

        currentFlipBook.on('init', (e) => {
            if (pageIndicator) {
                pageIndicator.textContent = `Page 1 of ${totalPages}`;
            }
        });

    } catch (err) {
        console.error('Error rendering 3D flipbook:', err);
        if (loadingElem) {
            loadingElem.innerHTML = `<p style="color: #ef4444;">Failed to load PDF in 3D reader. <a href="${url}" target="_blank" style="color: white; text-decoration: underline;">Open standard PDF</a></p>`;
        }
    }
}

/**
 * Flipbook navigation helpers
 */
function flipNextPage() {
    if (currentFlipBook) {
        currentFlipBook.flipNext('top');
    }
}

function flipPrevPage() {
    if (currentFlipBook) {
        currentFlipBook.flipPrev('top');
    }
}

/**
 * Toggle Fullscreen
 */
function toggleFullscreen() {
    const modal = document.getElementById('pdfModal');
    if (!document.fullscreenElement) {
        if (modal.requestFullscreen) {
            modal.requestFullscreen();
        } else if (modal.webkitRequestFullscreen) {
            modal.webkitRequestFullscreen();
        }
    } else {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        }
    }
}

/**
 * Close Reader Modal
 */
function closePdfReader() {
    const modal = document.getElementById('pdfModal');
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
    if (currentFlipBook) {
        try { currentFlipBook.destroy(); } catch (e) {}
        currentFlipBook = null;
    }
    if (document.fullscreenElement) {
        document.exitFullscreen().catch(() => {});
    }
}

// Keyboard shortcuts for page turning
document.addEventListener('keydown', (e) => {
    const modal = document.getElementById('pdfModal');
    if (modal && modal.classList.contains('active')) {
        if (e.key === 'ArrowRight' || e.key === 'PageDown' || e.key === ' ') {
            flipNextPage();
        } else if (e.key === 'ArrowLeft' || e.key === 'PageUp') {
            flipPrevPage();
        } else if (e.key === 'Escape') {
            closePdfReader();
        }
    }
});
