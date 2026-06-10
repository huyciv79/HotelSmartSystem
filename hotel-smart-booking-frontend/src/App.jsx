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
import Offers from './pages/Offers';
import Residences from './pages/Residences';
import Events from './pages/Events';
import Register from './pages/Register';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import Dashboard from './pages/Dashboard';
import Booking from './pages/Booking';
import Payment from './pages/Payment';
import BookingDetail from './pages/BookingDetail';

function DashboardRoute() {
  const navigate = useNavigate();

  const setActivePage = useCallback(
    (page) => {
      if (page === 'dashboard') return;
      navigate('/', { state: { page } });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [navigate],
  );

  return <Dashboard setActivePage={setActivePage} />;
}

function MainSite() {
  const navigate = useNavigate();
  const location = useLocation();
  const [activePage, setActivePageState] = useState(
    () => location.state?.page ?? 'home',
  );
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (location.state?.page) {
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
      if (page === 'dashboard') {
        navigate('/dashboard');
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
      case 'events':
        return <Events />;
      case 'offers':
        return <Offers />;
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
      case 'payment':
        return <Payment setActivePage={setActivePage} />;
      case 'booking-detail':
        return <BookingDetail setActivePage={setActivePage} />;
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
      <Footer setActivePage={setActivePage} />
    </div>
  );
}

function App() {
  return (
    <Routes>
      <Route path="/dashborad" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<DashboardRoute />} />
      <Route path="*" element={<MainSite />} />
    </Routes>
  );
}

export default App;
