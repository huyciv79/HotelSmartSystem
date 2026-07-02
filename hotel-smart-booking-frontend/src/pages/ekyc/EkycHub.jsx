import { useState, useCallback } from 'react';
import ViewEkyc from './ViewEkyc';
import RegisterEkycWizard from '../../components/ekyc/RegisterEkycWizard';
import { submitEkyc } from '../../services/ekycService';

// Internal screens
const SCREEN = {
  VIEW: 'view',
  REGISTER: 'register',
  UPDATE: 'update',
};

/**
 * Top-level eKYC hub. Manages navigation between all sub-screens. 
 * @param {() => void} onBack - Navigate back to dashboard/parent
 */
export default function EkycHub({ onBack }) {
  const [screen, setScreen] = useState(SCREEN.VIEW);
  const [wizardMode, setWizardMode] = useState('register'); // 'register' | 'update'

  const openRegister = useCallback(() => {
    setWizardMode('register');
    setScreen(SCREEN.REGISTER);
  }, []);

  const openUpdate = useCallback(() => {
    setWizardMode('update');
    setScreen(SCREEN.UPDATE);
  }, []);

  const handleSubmit = useCallback(async (front, back, faceFrames) => {
    return submitEkyc(front, back, faceFrames);
  }, []);

  const closeWizard = useCallback(() => {
    setScreen(SCREEN.VIEW);
  }, []);

  return (
    <div className="min-h-full">
      {/* Register / Update wizard (modal overlay) */}
      {(screen === SCREEN.REGISTER || screen === SCREEN.UPDATE) && (
        <RegisterEkycWizard
          mode={wizardMode}
          onSubmit={handleSubmit}
          onClose={closeWizard}
        />
      )}

      {/* ViewEkyc */}
      {screen === SCREEN.VIEW && (
        <ViewEkyc
          onBack={onBack}
          onRegister={openRegister}
          onUpdate={openUpdate}
        />
      )}
    </div>
  );
}
