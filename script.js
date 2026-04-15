// ── ReNu Tech — Shared Cart Utilities ──────────────────────────
// Include this script on every page: <script src="cart-utils.js"></script>

// Search functionality
const searchBtn = document.querySelector('.nav-buttons .btn-ghost:first-child');

// Create search overlay
const searchOverlay = document.createElement('div');
searchOverlay.classList.add('search-overlay');
searchOverlay.innerHTML = `
  <div class="search-box">
    <button class="search-close"><i class="fas fa-times"></i></button>
    <form class="search-form">
      <input type="text" class="search-input" placeholder="Search for products..." />
      <button type="submit" class="search-submit">
        <i class="fas fa-search"></i>
      </button>
    </form>
    <div class="search-results"></div>
  </div>
`;
document.body.appendChild(searchOverlay);

// Open search overlay
searchBtn.addEventListener('click', (e) => {
  e.preventDefault();
  searchOverlay.classList.add('active');
  searchOverlay.querySelector('.search-input').focus();
});

// Close search overlay
searchOverlay.querySelector('.search-close').addEventListener('click', () => {
  searchOverlay.classList.remove('active');
});

// Close when clicking outside the box
searchOverlay.addEventListener('click', (e) => {
  if (e.target === searchOverlay) {
    searchOverlay.classList.remove('active');
  }
});

// Close on Escape key
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') searchOverlay.classList.remove('active');
});

// Handle search submit
searchOverlay.querySelector('.search-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const query = searchOverlay.querySelector('.search-input').value.toLowerCase();
  const resultsDiv = searchOverlay.querySelector('.search-results');

  // Your products list — expand this as you add more
  const products = [
    { name: 'iPhone 16 Pro Neutral Titanium', price: '$699', link: '/ReNu-Project/product-detail.html?id=1' },
    { name: 'iPhone 15 Pro Black Titanium', price: '$489', link: '/ReNu-Project/product-detail.html?id=2' },
    { name: 'iPhone 17 Blue', price: '$749', link: '/ReNu-Project/product-detail.html?id=3' },
    { name: 'MacBook Pro M4 Space Black - 14 inch', price: '$1,299', link: '/ReNu-Project/product-detail.html?id=4' },
    { name: 'MacBook Air M2 Midnight 13"', price: '$749', link: '/ReNu-Project/product-detail.html?id=5' },
    { name: 'MacBook Pro M3 Silver 14"', price: '$999', link: '/ReNu-Project/product-detail.html?id=6' },
    { name: 'Apple Watch Ultra 3 - Natural', price: '$399', link: '/ReNu-Project/product-detail.html?id=7' },
    { name: 'Apple Watch SE 2nd Gen - Midnight', price: '$169', link: '/ReNu-Project/product-detail.html?id=8' },
    { name: 'Apple Watch S11 - Midnight', price: '$269', link: '/ReNu-Project/product-detail.html?id=9' },

    // Add more products here as your store grows
  ];

  const matches = products.filter(p => p.name.toLowerCase().includes(query));

  if (matches.length === 0) {
    resultsDiv.innerHTML = `<p class="no-results">No products found for "<strong>${query}</strong>"</p>`;
  } else {
    resultsDiv.innerHTML = matches.map(p => `
      <a href="${p.link}" class="search-result-item">
        <i class="fas fa-box"></i>
        <div>
          <div class="result-name">${p.name}</div>
          <div class="result-price">${p.price}</div>
        </div>
      </a>
    `).join('');
  }
});

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
function getProductIdFromName(name) {
    const productMap = {
        'iPhone 16 Pro': 1,
        'iPhone 15 Pro': 2,
        'iPhone 17': 3,
        'MacBook Pro M4 Space Black 14"': 4,
        'MacBook Air M2 Midnight 13"': 5,
        'MacBook Pro M3 Silver 14"': 6,
        'Apple Watch Ultra 3': 7,
        'Apple Watch SE 2nd Gen': 8,
        'Apple Watch S11': 9
    };

    return productMap[name] || null;
}

function addToCart(item) {
    const cart = getCart();

    const normalizedItem = {
        ...item,
        productId: item.productId ?? getProductIdFromName(item.name)
    };

    const idx = cart.findIndex(x => x.id === normalizedItem.id);

    if (idx > -1) {
        cart[idx].qty += normalizedItem.qty;
        if (!cart[idx].productId) {
            cart[idx].productId = normalizedItem.productId;
        }
    } else {
        cart.push(normalizedItem);
    }

    saveCart(cart);
    updateCartBadge();
}

// Run on every page load to keep badge in sync
document.addEventListener('DOMContentLoaded', updateCartBadge);
