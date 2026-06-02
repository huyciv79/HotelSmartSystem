import { useState } from 'react';
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

function App() {
  const [activePage, setActivePage] = useState('home');

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
      default:
        return <Home setActivePage={setActivePage} />;
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-background text-on-surface">
      <Navbar activePage={activePage} setActivePage={setActivePage} />
      
      <main className="flex-grow">
        {renderPage()}
      </main>

      <Footer setActivePage={setActivePage} />
    </div>
  );
}

export default App;
