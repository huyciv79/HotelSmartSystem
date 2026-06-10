import { useState, useEffect } from 'react';
import { useToast, ToastContainer } from '../components/Toast';

export default function Payment({ setActivePage }) {
  const { toasts, showToast, dismissToast } = useToast();
  
  const [booking, setBooking] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('card'); // 'card' | 'qr' | 'bank'
  const [cardNumber, setCardNumber] = useState('');
  const [cardName, setCardName] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  
  const [isProcessing, setIsProcessing] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    const savedBookingStr = sessionStorage.getItem('currentBooking');
    if (savedBookingStr) {
      const parsed = JSON.parse(savedBookingStr);
      setTimeout(() => {
        setBooking(parsed);
      }, 0);
    } else {
      showToast('Không tìm thấy thông tin đặt phòng cần thanh toán.', 'error');
      setTimeout(() => {
        setActivePage('home');
      }, 1500);
    }
  }, [setActivePage, showToast]);

  if (!booking) {
    return (
      <div className="w-full min-h-screen pt-36 pb-24 bg-gray-50 flex items-center justify-center font-['Montserrat']">
        <div className="text-center">
          <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-primary mx-auto mb-4"></div>
          <p className="text-xs uppercase font-bold tracking-widest text-slate-500">Đang tải thông tin thanh toán...</p>
        </div>
      </div>
    );
  }

  const handlePaymentSubmit = (e) => {
    e.preventDefault();
    
    if (paymentMethod === 'card') {
      if (!cardNumber || !cardName || !cardExpiry || !cardCvv) {
        showToast('Vui lòng điền đầy đủ thông tin thẻ tín dụng.', 'error');
        return;
      }
    }

    setIsProcessing(true);
    
    // Simulate premium payment processing
    setTimeout(() => {
      setIsProcessing(false);
      setIsSuccess(true);
      showToast('Thanh toán thành công! Chào mừng bạn đến với Elysian.', 'success');
      // Clean current booking session
      sessionStorage.removeItem('currentBooking');
    }, 2500);
  };

  const checkInMethodText = (method) => {
    switch(method) {
      case 'FaceID': return 'FaceID eKYC';
      case 'QR Code': return 'Mã QR Code';
      default: return 'Tại quầy lễ tân';
    }
  };

  const checkInInstruction = (method) => {
    switch(method) {
      case 'FaceID':
        return 'Bạn đã chọn nhận phòng FaceID. Vui lòng thiết lập hồ sơ eKYC (Face Embedding) trong trang cá nhân Dashboard để nhận phòng tự động bằng khuôn mặt khi đến khách sạn.';
      case 'QR Code':
        return 'Bạn đã chọn nhận phòng bằng QR Code. Mã nhận phòng QR Code đã được tạo và lưu trong Dashboard. Vui lòng xuất trình mã QR Code tại ki-ốt tự động khi nhận phòng.';
      default:
        return 'Vui lòng xuất trình giấy tờ tùy thân tại quầy lễ tân Elysian Hotels để làm thủ tục nhận phòng trực tiếp.';
    }
  };

  return (
    <>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
      <div className="w-full min-h-screen pt-36 pb-24 bg-gray-50 flex items-start justify-center px-4 font-['Montserrat']">
        
        {isProcessing && (
          <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-[9999] flex flex-col items-center justify-center text-white">
            <div className="relative w-20 h-20 mb-6">
              <div className="absolute inset-0 border-4 border-white/20 rounded-full"></div>
              <div className="absolute inset-0 border-4 border-t-primary rounded-full animate-spin"></div>
            </div>
            <h3 className="text-sm font-black uppercase tracking-[0.2em] mb-2 text-primary">Đang xác thực giao dịch</h3>
            <p className="text-[10px] text-white/60 font-medium uppercase tracking-widest">Vui lòng không tắt hoặc tải lại trang này...</p>
          </div>
        )}

        {isSuccess ? (
          /* Premium Receipt Screen */
          <div className="max-w-2xl w-full bg-white border border-outline-variant shadow-2xl p-8 md:p-12 text-center animate-scale-in">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
              <span className="material-symbols-outlined text-primary text-3xl font-bold">check_circle</span>
            </div>
            <h2 className="font-headline-lg text-headline-md text-primary uppercase italic tracking-wider m-0 mb-2">
              THANH TOÁN THÀNH CÔNG!
            </h2>
            <p className="text-secondary text-[11px] font-bold uppercase tracking-widest border-b border-gray-100 pb-6 mb-8">
              Cảm ơn bạn đã lựa chọn Elysian Hotels làm điểm đến của hành trình
            </p>

            {/* Receipt Box */}
            <div className="bg-slate-50 border border-slate-200 text-left p-6 md:p-8 space-y-4 mb-8">
              <div className="flex justify-between border-b border-dashed border-slate-300 pb-3">
                <span className="text-[10px] text-slate-400 font-black uppercase tracking-wider">Mã đặt phòng (Ref)</span>
                <span className="text-xs font-black text-slate-900 uppercase tracking-widest">{booking.bookingReference}</span>
              </div>

              <div className="space-y-2 text-xs font-bold text-slate-700">
                <div className="flex justify-between">
                  <span className="text-slate-400 font-bold uppercase tracking-wider">Hạng phòng:</span>
                  <span className="uppercase">{booking.roomTypeName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-bold uppercase tracking-wider">Thời gian:</span>
                  <span>{booking.checkInDate} đến {booking.checkOutDate} ({booking.nights} đêm)</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-bold uppercase tracking-wider">Phương thức nhận phòng:</span>
                  <span className="text-primary uppercase">{checkInMethodText(booking.checkInMethod)}</span>
                </div>
                <div className="flex justify-between border-t border-dashed border-slate-300 pt-3 text-sm">
                  <span className="font-black text-slate-900 uppercase tracking-wider">Tổng số tiền thanh toán:</span>
                  <span className="font-black text-primary text-base">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(booking.finalAmount || (booking.totalAmount * 1.1))}
                  </span>
                </div>
              </div>

              {/* Check-in Guidance */}
              <div className="bg-primary/5 border-l-2 border-primary p-4 text-[10px] text-slate-700 font-medium leading-relaxed mt-4">
                <span className="block font-black text-primary uppercase tracking-widest mb-1">Hướng dẫn nhận phòng:</span>
                {checkInInstruction(booking.checkInMethod)}
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button
                onClick={() => setActivePage('dashboard')}
                className="bg-primary text-on-primary font-bold px-8 py-3.5 text-xs uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 h-12"
              >
                <span className="material-symbols-outlined text-sm">dashboard</span> Xem quản lý đặt phòng
              </button>
              <button
                onClick={() => setActivePage('home')}
                className="bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold px-8 py-3.5 text-xs uppercase tracking-widest active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 h-12"
              >
                <span className="material-symbols-outlined text-sm">home</span> Trở về trang chủ
              </button>
            </div>
          </div>
        ) : (
          /* Payment Selection & Card Details Form */
          <div className="max-w-6xl w-full grid grid-cols-1 lg:grid-cols-12 gap-8 text-left animate-fade-in-up">
            
            {/* Left Column: Choose Payment Method & Form (col-span-7) */}
            <div className="lg:col-span-7 bg-white p-8 md:p-10 border border-outline-variant shadow-lg flex flex-col">
              <button 
                onClick={() => setActivePage('booking')}
                className="mb-6 w-fit text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
              >
                <span className="material-symbols-outlined text-sm">arrow_back</span> Quay lại chỉnh sửa
              </button>

              <h2 className="font-headline-lg text-headline-md text-primary uppercase italic tracking-wider m-0 mb-2">
                THANH TOÁN ĐẶT PHÒNG
              </h2>
              <p className="text-secondary text-xs font-bold uppercase tracking-widest border-b border-gray-100 pb-4 mb-6">
                Lựa chọn phương thức thanh toán an toàn để hoàn tất đặt phòng
              </p>

              {/* Payment Methods selector */}
              <div className="flex border-b border-slate-200 mb-6">
                <button
                  type="button"
                  onClick={() => setPaymentMethod('card')}
                  className={`flex-1 py-3.5 text-center text-xs font-extrabold uppercase tracking-widest transition-all cursor-pointer border-none bg-transparent ${
                    paymentMethod === 'card' 
                      ? 'text-primary border-b-2 border-primary' 
                      : 'text-slate-500 hover:text-primary'
                  }`}
                >
                  Thẻ tín dụng
                </button>
                <button
                  type="button"
                  onClick={() => setPaymentMethod('qr')}
                  className={`flex-1 py-3.5 text-center text-xs font-extrabold uppercase tracking-widest transition-all cursor-pointer border-none bg-transparent ${
                    paymentMethod === 'qr' 
                      ? 'text-primary border-b-2 border-primary' 
                      : 'text-slate-500 hover:text-primary'
                  }`}
                >
                  Mã QR Pay
                </button>
                <button
                  type="button"
                  onClick={() => setPaymentMethod('bank')}
                  className={`flex-1 py-3.5 text-center text-xs font-extrabold uppercase tracking-widest transition-all cursor-pointer border-none bg-transparent ${
                    paymentMethod === 'bank' 
                      ? 'text-primary border-b-2 border-primary' 
                      : 'text-slate-500 hover:text-primary'
                  }`}
                >
                  Chuyển khoản
                </button>
              </div>

              {/* Payment Form Submission */}
              <form onSubmit={handlePaymentSubmit} className="space-y-6">
                {paymentMethod === 'card' && (
                  <div className="space-y-6">
                    {/* Card Number */}
                    <div className="space-y-2">
                      <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Số thẻ</label>
                      <input 
                        type="text"
                        placeholder="4111 2222 3333 4444"
                        value={cardNumber}
                        onChange={(e) => setCardNumber(e.target.value.replace(/\D/g, '').replace(/(\d{4})(?=\d)/g, '$1 '))}
                        maxLength="19"
                        className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                      />
                    </div>

                    {/* Card Name */}
                    <div className="space-y-2">
                      <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Tên trên thẻ</label>
                      <input 
                        type="text"
                        placeholder="NGUYEN VAN A"
                        value={cardName}
                        onChange={(e) => setCardName(e.target.value.toUpperCase())}
                        className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                      />
                    </div>

                    {/* Expiry & CVV */}
                    <div className="grid grid-cols-2 gap-6">
                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Hạn dùng (MM/YY)</label>
                        <input 
                          type="text"
                          placeholder="12/28"
                          value={cardExpiry}
                          onChange={(e) => {
                            let val = e.target.value.replace(/\D/g, '');
                            if (val.length > 2) val = val.substring(0, 2) + '/' + val.substring(2, 4);
                            setCardExpiry(val);
                          }}
                          maxLength="5"
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="block text-xs font-bold text-secondary uppercase tracking-widest">CVV / CVC</label>
                        <input 
                          type="password"
                          placeholder="•••"
                          value={cardCvv}
                          onChange={(e) => setCardCvv(e.target.value.replace(/\D/g, ''))}
                          maxLength="3"
                          className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary"
                        />
                      </div>
                    </div>
                  </div>
                )}

                {paymentMethod === 'qr' && (
                  <div className="space-y-4 text-center py-6 bg-slate-50 border border-slate-200">
                    <div className="w-48 h-48 bg-white border border-slate-300 mx-auto flex items-center justify-center relative p-3">
                      {/* Simulated QR Code placeholder using custom vector styling */}
                      <span className="material-symbols-outlined text-[120px] text-slate-800">qr_code_2</span>
                    </div>
                    <div>
                      <p className="text-xs font-black text-slate-900 uppercase tracking-widest mb-1">Quét mã QR qua ứng dụng Ngân hàng / Ví điện tử</p>
                      <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Hệ thống hỗ trợ VNPAY-QR, MoMo, ShopeePay, Moca</p>
                    </div>
                  </div>
                )}

                {paymentMethod === 'bank' && (
                  <div className="space-y-4 bg-slate-50 border border-slate-200 p-6 text-slate-700">
                    <p className="text-[11px] font-black text-slate-900 uppercase tracking-widest border-b border-slate-200 pb-2 mb-3">Thông tin chuyển khoản ngân hàng</p>
                    <div className="space-y-2.5 text-xs font-bold">
                      <div className="flex justify-between">
                        <span className="text-slate-400 font-bold uppercase tracking-wider">Ngân hàng:</span>
                        <span>VIETCOMBANK (VCB)</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400 font-bold uppercase tracking-wider">Số tài khoản:</span>
                        <span className="text-slate-950 font-black tracking-wider">1029 8888 9999</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400 font-bold uppercase tracking-wider">Chủ tài khoản:</span>
                        <span>ELYSIAN HOTELS & RESORTS JSC</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400 font-bold uppercase tracking-wider">Số tiền:</span>
                        <span className="text-primary font-black">
                          {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(booking.finalAmount || (booking.totalAmount * 1.1))}
                        </span>
                      </div>
                      <div className="flex justify-between border-t border-slate-200 pt-3">
                        <span className="text-slate-400 font-bold uppercase tracking-wider">Nội dung chuyển khoản:</span>
                        <span className="text-primary font-black tracking-widest uppercase">{booking.bookingReference}</span>
                      </div>
                    </div>
                    <div className="bg-primary/5 p-3 text-[9px] text-slate-500 font-semibold leading-relaxed border-l-2 border-primary uppercase tracking-wider mt-4">
                      Sau khi chuyển khoản thành công, vui lòng giữ biên lai và chờ hệ thống xác nhận tự động trong vòng 2-5 phút.
                    </div>
                  </div>
                )}

                {/* Submit button */}
                <button 
                  type="submit"
                  className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-2 h-12"
                >
                  {paymentMethod === 'card' ? 'XÁC NHẬN THANH TOÁN THẺ' : paymentMethod === 'qr' ? 'TÔI ĐÃ QUÉT MÃ QR THÀNH CÔNG' : 'TÔI ĐÃ CHUYỂN KHOẢN THÀNH CÔNG'}
                </button>
              </form>
            </div>

            {/* Right Column: Invoice Details (col-span-5) */}
            <div className="lg:col-span-5 bg-white border border-outline-variant shadow-lg p-8 flex flex-col justify-between h-fit">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest mb-4 border-b border-gray-100 pb-2">
                  Chi Tiết Hóa Đơn
                </h3>

                <div className="space-y-4">
                  {/* Summary Details */}
                  <div className="space-y-3.5 border-b border-gray-100 pb-4 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Mã Booking:</span>
                      <span className="uppercase tracking-widest">{booking.bookingReference}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Hạng phòng:</span>
                      <span className="uppercase">{booking.roomTypeName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Ngày Nhận phòng:</span>
                      <span>{booking.checkInDate}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Ngày Trả phòng:</span>
                      <span>{booking.checkOutDate}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Phương thức nhận:</span>
                      <span>{checkInMethodText(booking.checkInMethod)}</span>
                    </div>
                  </div>

                  {/* Payment totals */}
                  <div className="space-y-2 pt-2 text-xs font-bold text-slate-700">
                    <div className="flex justify-between border-t border-slate-900 pt-3 text-sm">
                      <span className="font-black text-slate-900 uppercase tracking-wider">TỔNG CỘNG CẦN THANH TOÁN:</span>
                      <span className="font-black text-primary text-base">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(booking.finalAmount || (booking.totalAmount * 1.1))}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Safety notice */}
              <div className="mt-8 border border-slate-150 p-4 text-[9px] text-slate-500 font-semibold leading-relaxed uppercase tracking-wider flex items-start gap-2">
                <span className="material-symbols-outlined text-base text-slate-400">security</span>
                <span>Thông tin thanh toán của bạn được bảo mật tuyệt đối bởi tiêu chuẩn mã hóa dữ liệu PCI DSS tối cao.</span>
              </div>
            </div>
            
          </div>
        )}
      </div>
    </>
  );
}
