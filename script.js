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
    { name: 'MacBook Pro M4 Space Black 14"', price: '$1,299', link: '/ReNu-Project/product-detail.html?id=4' },
    { name: 'MacBook Air M4 Midnight 13"', price: '$749', link: '/ReNu-Project/product-detail.html?id=5' },
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
function addToCart(item) {
    // 1. Get the current cart from localStorage
    let cart = JSON.parse(localStorage.getItem('renuCart') || '[]');

    // 2. Check if the item is already in the cart to get current quantity
    const existingItem = cart.find(cartItem => cartItem.id === item.id);
    const currentQtyInCart = existingItem ? existingItem.quantity : 0;

    // 3. INVENTORY CHECK (The New Part)
    // We check if adding 1 more exceeds the 'stock' from your MySQL database
    if (currentQtyInCart + 1 > item.stock) {
        alert(`Sorry! We only have ${item.stock} in stock. You already have ${currentQtyInCart} in your cart.`);
        return; // Stop here so nothing is added
    }

    // 4. SURROUNDING CODE (Your original logic)
    if (existingItem) {
        // If it's already there, just bump the number
        existingItem.quantity += 1;
    } else {
        // If it's new, add it to the list with quantity 1
        // We use the spread operator (...) to keep all the product data
        cart.push({ ...item, quantity: 1 });
    }

    // 5. UPDATE STORAGE & UI
    localStorage.setItem('renuCart', JSON.stringify(cart));
    
    // Call your existing function to update the red bubble on the cart icon
    if (typeof updateCartCount === "function") {
        updateCartCount();
    }

    // Optional: Log to console so you can see it working in DevTools
    console.log(`Added ${item.name} to cart. Total in cart: ${currentQtyInCart + 1}`);
}
//------------Old cart functions---------------------------
/*function getCart() {
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
document.addEventListener('DOMContentLoaded', updateCartBadge); */

// ── Admin Preview Banner ──────────────────────────────────────
if (sessionStorage.getItem('adminPreview') === 'true') {
    const bar = document.createElement('div');
    bar.id = 'adminPreviewBar';
    bar.innerHTML = `
        <style>
            #adminPreviewBar {
                position: fixed; bottom: 0; left: 0; right: 0; z-index: 99999;
                background: #3A3A3A; color: #fff;
                display: flex; align-items: center; justify-content: center; gap: 16px;
                padding: 12px 20px; font-family: 'Montserrat', sans-serif;
                font-size: 13px; font-weight: 600;
                box-shadow: 0 -4px 20px rgba(0,0,0,0.3);
            }
            #adminPreviewBar i { color: #72AEC8; }
            #adminPreviewBar a {
                background: #72AEC8; color: #fff; padding: 7px 18px;
                border-radius: 20px; text-decoration: none; font-weight: 700;
                font-size: 12px; transition: background 0.2s;
            }
            #adminPreviewBar a:hover { background: #5a9ab5; }
            #adminPreviewBar button {
                background: none; border: 1px solid rgba(255,255,255,0.3);
                color: #ccc; padding: 6px 14px; border-radius: 20px;
                cursor: pointer; font-size: 12px; font-weight: 600;
                font-family: 'Montserrat', sans-serif;
            }
        </style>
        <i class="fas fa-eye"></i>
        <span>You are viewing the store as a customer (Admin Preview)</span>
        <a href="/admin.html"><i class="fas fa-arrow-left"></i> Back to Admin</a>
        <button onclick="sessionStorage.removeItem('adminPreview'); this.closest('#adminPreviewBar').remove()">Dismiss</button>
    `;
    document.body.appendChild(bar);
    // Push page content up so the bar doesn't cover the footer
    document.body.style.paddingBottom = '56px';
}
// ── User Auth UI ─────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    const user = JSON.parse(sessionStorage.getItem('renutech_user') || 'null');
    const userBtn = document.querySelector('.nav-buttons .btn-ghost:nth-child(2)');
    if (!userBtn) return;

    if (user) {
        // Replace the plain icon with a dropdown
        userBtn.style.position = 'relative';
        userBtn.innerHTML = `<i class="fas fa-user-circle" style="color:var(--icons-prices-color)"></i>`;
        userBtn.href = '#';

        const dropdown = document.createElement('div');
        dropdown.id = 'userDropdown';
      dropdown.innerHTML = `
            <div class="user-drop-name">${user.name || 'User'}</div>
            <div class="user-drop-id">${user.id || 'GUEST'}</div>
            <a href="account.html" class="user-drop-item"><i class="fas fa-user"></i> View Account</a>
            <div class="user-drop-divider"></div>
            <a href="#" class="user-drop-item danger" id="logoutLink"><i class="fas fa-sign-out-alt"></i> Logout</a>
        `;
      
        dropdown.style.cssText = `
            display:none; position:absolute; top:calc(100% + 10px); right:0;
            background:#fff; border:1.5px solid #e0e7ea; border-radius:12px;
            padding:8px; min-width:200px; box-shadow:0 8px 30px rgba(0,0,0,0.12);
            z-index:9999; font-family:'Montserrat',sans-serif;
        `;
        userBtn.appendChild(dropdown);
      // 1. Stop the menu from closing when you click inside it
      dropdown.addEventListener('click', (e) => {
          e.stopPropagation();
      });

      // 2. Ensure the "View Account" link specifically works
      const viewAccountLink = dropdown.querySelector('a[href="account.html"]');
      if (viewAccountLink) {
          viewAccountLink.addEventListener('click', (e) => {
        // Force navigation to account.html
            window.location.href = 'account.html';
        });
      }

        // Inject dropdown styles
        if (!document.getElementById('userDropStyles')) {
            const s = document.createElement('style');
            s.id = 'userDropStyles';
            s.textContent = `
                .user-drop-name { font-size:13px; font-weight:800; color:#272727; padding:6px 10px 2px; }
                .user-drop-id   { font-size:10px; font-weight:700; color:#72AEC8; padding:0 10px 8px; border-bottom:1px solid #f0f4f6; margin-bottom:4px; }
                .user-drop-item { display:flex; align-items:center; gap:10px; padding:9px 10px; border-radius:8px;
                    text-decoration:none; font-size:12px; font-weight:700; color:#272727; transition:background 0.15s; }
                .user-drop-item:hover { background:#EDF1F3; }
                .user-drop-item.danger { color:#e07070; }
                .user-drop-item.danger:hover { background:#fff5f5; }
                .user-drop-divider { height:1px; background:#f0f4f6; margin:4px 0; }
            `;
            document.head.appendChild(s);
        }

        // 1. Toggle dropdown visibility
        userBtn.addEventListener('click', (e) => {
            // If the user clicked the icon/button (not the dropdown content)
            if (e.target.closest('#userDropdown') === null) {
                e.preventDefault();
                const isOpen = dropdown.style.display === 'block';
                dropdown.style.display = isOpen ? 'none' : 'block';
            }
        });
        // 2. STOP PROPAGATION (The "Don't Close Me" Fix)
        // This ensures clicking the link doesn't trigger the button toggle
        dropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });
        // 3. Close when clicking anywhere else on the page
        document.addEventListener('click', (e) => {
            if (!userBtn.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
      
      const logoutBtn = document.getElementById('logoutLink');
      if (logoutBtn) {
          logoutBtn.addEventListener('click', (e) => {
              e.preventDefault();
              sessionStorage.removeItem('renutech_user');
              window.location.href = 'login.html';
          });
      }
        /* Logout
        document.getElementById('logoutLink').addEventListener('click', (e) => {
            e.preventDefault();
            sessionStorage.removeItem('renutech_user');
            window.location.href = 'login.html';
        }); */

    } else {
        // Not logged in — plain link to login
        userBtn.href = 'login.html';
    }
});
