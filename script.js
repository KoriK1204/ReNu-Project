// ── ReNu Tech — Shared Cart Utilities ──────────────────────────
// Include this script on every page: <script src="cart-utils.js"></script>

function getCart() {
    return JSON.parse(localStorage.getItem('renuCart') || '[]');
}

function saveCart(cart) {
    localStorage.setItem('renuCart', JSON.stringify(cart));
}

// Updates every element with id="cartCount" on the page
function updateCartBadge() {
    const total = getCart().reduce((sum, item) => sum + item.qty, 0);
    document.querySelectorAll('#cartCount').forEach(el => {
        el.textContent = total;
    });
}

// Adds an item to cart (merges qty if same id already exists)
function addToCart(item) {
    const cart = getCart();
    const idx = cart.findIndex(x => x.id === item.id);
    if (idx > -1) {
        cart[idx].qty += item.qty;
    } else {
        cart.push(item);
    }
    saveCart(cart);
    updateCartBadge();
}

// Run on every page load to keep badge in sync
document.addEventListener('DOMContentLoaded', updateCartBadge);
