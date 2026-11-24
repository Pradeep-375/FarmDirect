import { Navigate, Route, Routes } from 'react-router-dom';
import { HOME, useAuth } from './auth';
import Layout, { Shell } from './components/Layout';
import Home from './pages/Home';
import { About, Contact } from './pages/Static';
import { ProductDetail, Products } from './pages/Products';
import { FarmerPublic, Farmers } from './pages/Farmers';
import { Login, Register } from './pages/Auth';
import { Cart, Checkout, CustomerHome, CustomerProfile, Orders, TrackOrder, Wishlist } from './pages/customer/Customer';
import { Earnings, FarmerHome, FarmerOrders, FarmerProfile, FarmerTrack, ManageProducts, ProductForm } from './pages/farmer/Farmer';
import { DeliveryHistory, DeliveryHome } from './pages/delivery/Delivery';
import * as A from './pages/admin/Admin';

function Guard({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to={HOME[user.role]} replace />;
  return children;
}
const shell = (role, title, links) => <Guard role={role}><Shell title={title} links={links} /></Guard>;

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} /><Route path="about" element={<About />} /><Route path="contact" element={<Contact />} />
        <Route path="products" element={<Products />} /><Route path="products/:id" element={<ProductDetail />} />
        <Route path="farmers" element={<Farmers />} /><Route path="farmers/:id" element={<FarmerPublic />} />
        <Route path="login" element={<Login />} /><Route path="register" element={<Register />} />

        <Route path="customer" element={shell('CUSTOMER', 'My account', [['/customer', 'Dashboard', true], ['/customer/cart', 'Cart'], ['/customer/wishlist', 'Wishlist'], ['/customer/orders', 'Orders'], ['/customer/profile', 'Profile']])}>
          <Route index element={<CustomerHome />} /><Route path="cart" element={<Cart />} /><Route path="wishlist" element={<Wishlist />} /><Route path="checkout" element={<Checkout />} />
          <Route path="orders" element={<Orders />} /><Route path="track/:id" element={<TrackOrder />} /><Route path="profile" element={<CustomerProfile />} /></Route>

        <Route path="farmer" element={shell('FARMER', 'My farm', [['/farmer', 'Dashboard', true], ['/farmer/products', 'Products and stock'], ['/farmer/products/new', 'Add product'], ['/farmer/orders', 'Orders'], ['/farmer/earnings', 'Sales'], ['/farmer/profile', 'Farm profile']])}>
          <Route index element={<FarmerHome />} /><Route path="profile" element={<FarmerProfile />} /><Route path="products" element={<ManageProducts />} /><Route path="products/new" element={<ProductForm />} /><Route path="products/:id/edit" element={<ProductForm />} />
          <Route path="orders" element={<FarmerOrders />} /><Route path="orders/:id/tracking" element={<FarmerTrack />} /><Route path="earnings" element={<Earnings />} /></Route>

        <Route path="delivery" element={shell('DELIVERY_PARTNER', 'Deliveries', [['/delivery', 'Assigned', true], ['/delivery/history', 'History']])}>
          <Route index element={<DeliveryHome />} /><Route path="history" element={<DeliveryHistory />} /></Route>

        <Route path="admin" element={shell('ADMIN', 'Administration', [['/admin', 'Dashboard', true], ['/admin/farmers', 'Farmers'], ['/admin/verification', 'Verification'], ['/admin/customers', 'Customers'], ['/admin/partners', 'Delivery partners'], ['/admin/products', 'Products'], ['/admin/categories', 'Categories'], ['/admin/orders', 'Orders'], ['/admin/deliveries', 'Deliveries'], ['/admin/refunds', 'Refunds'], ['/admin/reviews', 'Reviews'], ['/admin/complaints', 'Complaints'], ['/admin/reports', 'Reports'], ['/admin/settings', 'Settings']])}>
          <Route index element={<A.AdminHome />} /><Route path="farmers" element={<A.Users role="FARMER" title="Farmers" />} /><Route path="customers" element={<A.Users role="CUSTOMER" title="Customers" />} /><Route path="partners" element={<A.Users role="DELIVERY_PARTNER" title="Delivery partners" />} />
          <Route path="verification" element={<A.Verification />} /><Route path="products" element={<A.AdminProducts />} /><Route path="categories" element={<A.Categories />} /><Route path="orders" element={<A.AdminOrders />} />
          <Route path="deliveries" element={<A.Deliveries />} /><Route path="refunds" element={<A.Refunds />} /><Route path="reviews" element={<A.AdminReviews />} /><Route path="complaints" element={<A.Complaints />} /><Route path="reports" element={<A.Reports />} /><Route path="settings" element={<A.Settings />} /></Route>

        <Route path="*" element={<p className="py-20 text-center">Page not found.</p>} />
      </Route>
    </Routes>
  );
}
