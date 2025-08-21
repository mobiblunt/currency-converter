// Currency Converter JavaScript

document.addEventListener('DOMContentLoaded', function() {
    
    // Form elements
    const conversionForm = document.getElementById('conversionForm');
    const swapBtn = document.getElementById('swapBtn');
    const convertBtn = document.getElementById('convertBtn');
    const fromCurrency = document.getElementById('fromCurrency');
    const toCurrency = document.getElementById('toCurrency');
    const amountInput = document.getElementById('amount');
    const loadingModal = new bootstrap.Modal(document.getElementById('loadingModal'));
    
    // Quick conversion buttons
    const quickConvertBtns = document.querySelectorAll('.quick-convert');
    
    // Swap currencies functionality
    if (swapBtn) {
        swapBtn.addEventListener('click', function() {
            const fromValue = fromCurrency.value;
            const toValue = toCurrency.value;
            
            // Add animation
            swapBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            
            setTimeout(() => {
                fromCurrency.value = toValue;
                toCurrency.value = fromValue;
                swapBtn.innerHTML = '<i class="fas fa-exchange-alt"></i>';
                
                // Add visual feedback
                fromCurrency.classList.add('border-success');
                toCurrency.classList.add('border-success');
                
                setTimeout(() => {
                    fromCurrency.classList.remove('border-success');
                    toCurrency.classList.remove('border-success');
                }, 1000);
            }, 500);
        });
    }
    
    // Form submission with loading
    if (conversionForm) {
        conversionForm.addEventListener('submit', function(e) {
            // Show loading modal
            loadingModal.show();
            
            // Disable form elements
            convertBtn.disabled = true;
            convertBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Converting...';
            
            // Add loading class to form
            conversionForm.classList.add('loading');
        });
    }
    
    // Quick conversion functionality
    quickConvertBtns.forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            
            const from = this.getAttribute('data-from');
            const to = this.getAttribute('data-to');
            
            // Set the form values
            fromCurrency.value = from;
            toCurrency.value = to;
            
            // Add visual feedback
            this.classList.add('btn-primary');
            this.classList.remove('btn-outline-primary');
            
            setTimeout(() => {
                this.classList.remove('btn-primary');
                this.classList.add('btn-outline-primary');
            }, 1000);
            
            // Focus on amount input
            amountInput.focus();
        });
    });
    
    // Auto-format amount input
    if (amountInput) {
        amountInput.addEventListener('input', function() {
            let value = this.value;
            
            // Remove any non-numeric characters except decimal point
            value = value.replace(/[^0-9.]/g, '');
            
            // Ensure only one decimal point
            const parts = value.split('.');
            if (parts.length > 2) {
                value = parts[0] + '.' + parts.slice(1).join('');
            }
            
            this.value = value;
        });
        
        // Add enter key support
        amountInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                conversionForm.submit();
            }
        });
    }
    
    // Currency validation
    function validateCurrencies() {
        if (fromCurrency.value === toCurrency.value) {
            toCurrency.classList.add('is-invalid');
            if (!document.querySelector('.same-currency-error')) {
                const errorDiv = document.createElement('div');
                errorDiv.className = 'invalid-feedback same-currency-error';
                errorDiv.textContent = 'Please select different currencies';
                toCurrency.parentNode.appendChild(errorDiv);
            }
            return false;
        } else {
            toCurrency.classList.remove('is-invalid');
            const errorDiv = document.querySelector('.same-currency-error');
            if (errorDiv) {
                errorDiv.remove();
            }
            return true;
        }
    }
    
    // Add currency validation listeners
    if (fromCurrency && toCurrency) {
        fromCurrency.addEventListener('change', validateCurrencies);
        toCurrency.addEventListener('change', validateCurrencies);
    }
    
    // Animate cards on load
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
        card.style.animationDelay = `${index * 0.1}s`;
    });
    
    // Hide loading modal if page loads with errors
    window.addEventListener('load', function() {
        setTimeout(() => {
            loadingModal.hide();
            if (convertBtn) {
                convertBtn.disabled = false;
                convertBtn.innerHTML = '<i class="fas fa-calculator me-2"></i>Convert Currency';
            }
            if (conversionForm) {
                conversionForm.classList.remove('loading');
            }
        }, 500);
    });
    
    // Add tooltips to buttons
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[title]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Real-time rate preview (optional feature)
    let ratePreviewTimeout;
    
    function showRatePreview() {
        const from = fromCurrency.value;
        const to = toCurrency.value;
        
        if (from && to && from !== to) {
            clearTimeout(ratePreviewTimeout);
            ratePreviewTimeout = setTimeout(() => {
                // This would make an AJAX call to get preview rate
                // For now, just show a loading indicator
                console.log(`Getting rate preview for ${from} to ${to}`);
            }, 1000);
        }
    }
    
    if (fromCurrency && toCurrency) {
        fromCurrency.addEventListener('change', showRatePreview);
        toCurrency.addEventListener('change', showRatePreview);
    }
});

// Utility functions
function formatCurrency(amount, currency) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount);
}

function showNotification(message, type = 'info') {
    // Create toast notification
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    // Add to page
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }
    
    toastContainer.appendChild(toast);
    
    // Show toast
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    // Remove from DOM after hiding
    toast.addEventListener('hidden.bs.toast', function() {
        toast.remove();
    });
}
 