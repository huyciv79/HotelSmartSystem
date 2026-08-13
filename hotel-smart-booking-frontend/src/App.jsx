import { useCallback, useEffect, useState } from 'react';
import {
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import Experiences from './pages/Experiences';
import Rewards from './pages/Rewards';
import Residences from './pages/Residences';
import Register from './pages/Register';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import Dashboard from './pages/Dashboard';
import Booking from './pages/Booking';
import Payment from './pages/Payment';
import BookingDetail from './pages/BookingDetail';
import GroupBooking from './pages/GroupBooking';
import StaffDashboard from './pages/StaffDashboard';
import AiAssistant from './pages/AiAssistant';
import AiFloatingBubble from './components/AiFloatingBubble';
import HotelPolicy from './pages/HotelPolicy';
import HotelTerms from './pages/HotelTerms';
import EkycHub from './pages/ekyc/EkycHub';
import ErrorBoundary from './components/ErrorBoundary';

function DashboardRoute() {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState(() => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  });

  const token = localStorage.getItem('accessToken');

  // Keep state in sync with local storage updates
  useEffect(() => {
    const userStr = localStorage.getItem('user');
    setCurrentUser(userStr ? JSON.parse(userStr) : null);
  }, []);

  const setActivePage = useCallback(
    (page) => {
      if (page === 'dashboard') return;
      navigate('/', { state: { page } });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [navigate],
  );

  // Protect route: Redirect unauthenticated users to Login page
  if (!token || !currentUser) {
    return <Navigate to="/" state={{ page: 'login' }} replace />;
  }

  if (currentUser && (currentUser.role === 'manager' || currentUser.role === 'receptionist')) {
    return <StaffDashboard setActivePage={setActivePage} />;
  }

  return (
    <>
      <Dashboard setActivePage={setActivePage} />
      <AiFloatingBubble setActivePage={setActivePage} activePage="dashboard" />
    </>
  );
}

function MainSite() {
  const navigate = useNavigate();
  const location = useLocation();
  const [activePage, setActivePageState] = useState(
    () => location.state?.page ?? 'home',
  );
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('token') && params.get('PayerID')) {
      setActivePageState('payment');
    } else if (location.state?.page) {
      const page = location.state.page;
      navigate('/', { replace: true });
      // Schedule state update to avoid synchronous setState inside render/effect cascade
      setTimeout(() => {
        setActivePageState(page);
      }, 0);
    }
  }, [location.state, navigate]);

  const setActivePage = useCallback(
    (page) => {
      if (page === 'dashboard' || page === 'ekyc') {
        const token = localStorage.getItem('accessToken');
        if (!token) {
          setActivePageState('login');
          window.scrollTo({ top: 0, behavior: 'smooth' });
          return;
        }
        navigate('/dashboard', { state: { tab: page === 'ekyc' ? 'ekyc' : 'overview' } });
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return;
      }
      setActivePageState(page);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [navigate],
  );

  const renderPage = () => {
    switch (activePage) {
      case 'home':
        return <Home setActivePage={setActivePage} />;
      case 'residences':
        return <Residences />;
      case 'experiences':
        return <Experiences setActivePage={setActivePage} />;
      case 'register':
        return <Register setActivePage={setActivePage} />;
      case 'login':
        return <Login setActivePage={setActivePage} />;
      case 'forgot-password':
        return <ForgotPassword setActivePage={setActivePage} />;
      case 'rewards':
        return <Rewards />;
      case 'booking':
        return <Booking setActivePage={setActivePage} />;
      case 'group-booking':
        return <GroupBooking setActivePage={setActivePage} />;
      case 'payment':
        return <Payment setActivePage={setActivePage} />;
      case 'booking-detail':
        return <BookingDetail setActivePage={setActivePage} />;
      case 'ai-assistant':
        return <AiAssistant setActivePage={setActivePage} />;
      case 'privacy-policy':
        return <HotelPolicy />;
      case 'terms-of-service':
        return <HotelTerms />;
      case 'ekyc':
        return <EkycHub onBack={() => setActivePage('dashboard')} />;
      default:
        return <Home setActivePage={setActivePage} />;
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-background text-on-surface">
      <Navbar 
        activePage={activePage} 
        setActivePage={setActivePage} 
        isMobileMenuOpen={isMobileMenuOpen}
        setIsMobileMenuOpen={setIsMobileMenuOpen}
      />

      <main className="flex-grow">{renderPage()}</main>
      <Footer activePage={activePage} setActivePage={setActivePage} />
      <AiFloatingBubble setActivePage={setActivePage} activePage={activePage} />
    </div>
  );
}

function App() {
  return (
    <ErrorBoundary>
      <Routes>
        <Route path="/dashborad" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardRoute />} />
        <Route path="/ekyc" element={<Navigate to="/dashboard" state={{ tab: 'ekyc' }} replace />} />
        <Route path="/register-ekyc" element={<Navigate to="/dashboard" state={{ tab: 'ekyc' }} replace />} />
        <Route path="/ekyc-register" element={<Navigate to="/dashboard" state={{ tab: 'ekyc' }} replace />} />
        <Route path="*" element={<MainSite />} />
      </Routes>
    </ErrorBoundary>
  );
}

export default App;