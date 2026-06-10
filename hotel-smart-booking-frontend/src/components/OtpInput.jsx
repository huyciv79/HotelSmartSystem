import { useRef, useEffect } from 'react';

export default function OtpInput({ value = '', onChange, disabled = false }) {
  const length = 6;
  const inputRefs = useRef([]);

  // Split value into an array of characters, padding with empty strings
  const digits = value.split('').concat(Array(length).fill('')).slice(0, length);

  const handleChange = (e, index) => {
    const val = e.target.value;
    // Allow only numeric digits
    if (val && !/^\d+$/.test(val)) return;

    // Keep only the last character (handles overwriting)
    const char = val.substring(val.length - 1);
    
    const newDigits = [...digits];
    newDigits[index] = char;
    
    const otpValue = newDigits.join('');
    if (onChange) {
      onChange(otpValue);
    }

    // Auto-focus next input field if filled
    if (char && index < length - 1 && inputRefs.current[index + 1]) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handleKeyDown = (e, index) => {
    if (e.key === 'Backspace') {
      const newDigits = [...digits];
      
      // If current field is empty, clear previous field and focus it
      if (!digits[index] && index > 0 && inputRefs.current[index - 1]) {
        newDigits[index - 1] = '';
        if (onChange) {
          onChange(newDigits.join(''));
        }
        inputRefs.current[index - 1].focus();
      } else {
        // Clear current field
        newDigits[index] = '';
        if (onChange) {
          onChange(newDigits.join(''));
        }
      }
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedText = e.clipboardData.getData('text').trim();
    
    // Allow only numeric pastes
    if (!/^\d+$/.test(pastedText)) return;

    const pastedDigits = pastedText.substring(0, length).split('');
    const newDigits = [...digits];
    
    pastedDigits.forEach((char, idx) => {
      newDigits[idx] = char;
    });

    const otpValue = newDigits.join('');
    if (onChange) {
      onChange(otpValue);
    }

    // Focus the last filled input box
    const focusIndex = Math.min(pastedDigits.length, length - 1);
    if (inputRefs.current[focusIndex]) {
      inputRefs.current[focusIndex].focus();
    }
  };

  // Ensure first input is focused when component is loaded (only if OTP value is empty)
  useEffect(() => {
    if (value === '' && inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="flex justify-center gap-3">
      {digits.map((digit, idx) => (
        <input
          key={idx}
          ref={(el) => { inputRefs.current[idx] = el; }}
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength={1}
          value={digit}
          onChange={(e) => handleChange(e, idx)}
          onKeyDown={(e) => handleKeyDown(e, idx)}
          onPaste={handlePaste}
          disabled={disabled}
          aria-label={`Mã xác thực ô ${idx + 1}`}
          className="w-12 h-14 border border-outline-variant text-center font-bold text-xl outline-none focus:border-primary focus:ring-1 focus:ring-primary bg-transparent text-on-surface disabled:opacity-50 transition-colors"
        />
      ))}
    </div>
  );
}
