/**
 * Interactive helper for Digital Magazine Portal
 */

function openPdfReader(url, title) {
    const modal = document.getElementById('pdfModal');
    const iframe = document.getElementById('pdfFrame');
    const titleElem = document.getElementById('modalTitle');
    const downloadBtn = document.getElementById('modalDownloadBtn');

    if (modal && iframe) {
        iframe.src = url;
        if (titleElem) titleElem.textContent = title || 'Magazine Viewer';
        
        // Derive download link from view link
        if (downloadBtn) {
            const downloadUrl = url.replace('/view/', '/download/');
            downloadBtn.href = downloadUrl;
        }

        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closePdfReader() {
    const modal = document.getElementById('pdfModal');
    const iframe = document.getElementById('pdfFrame');
    if (modal) {
        modal.classList.remove('active');
        if (iframe) iframe.src = '';
        document.body.style.overflow = '';
    }
}

// Close modal when pressing Escape
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closePdfReader();
    }
});
