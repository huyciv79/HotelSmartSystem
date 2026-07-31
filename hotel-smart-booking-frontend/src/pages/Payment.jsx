import { useState, useEffect, useRef } from 'react';
import { useToast, ToastContainer } from '../components/Toast';
import { useLanguage } from '../context/LanguageContext';
import axiosInstance from '../services/axiosInstance';

export default function Payment({ setActivePage }) {
  const { t } = useLanguage();
  const { toasts, showToast, dismissToast } = useToast();
  
  const [booking, setBooking] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('paypal');
  const [payOption, setPayOption] = useState('deposit');
  const [paidThisTime, setPaidThisTime] = useState(0);
  
  const [isProcessing, setIsProcessing] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const isCapturingRef = useRef(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const tokenParam = params.get('token');
    const payerIdParam = params.get('PayerID');

    if (tokenParam && payerIdParam) {
      if (isCapturingRef.current) return;
      isCapturingRef.current = true;
      setIsProcessing(true);
      capturePaypalPayment(tokenParam);
    } else {
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
    }
  }, [setActivePage, showToast, t]);

  const capturePaypalPayment = async (orderId) => {
    // Clear URL parameters immediately to prevent duplicate triggers on re-render/refresh
    window.history.replaceState({}, document.title, window.location.pathname);
    try {
      const response = await axiosInstance.post('/payments/paypal/capture-order', {
        paypalOrderId: orderId
      }, {
        headers: {
          'Idempotency-Key': `capture-${orderId}`
        }
      });
      const result = response.data;
      if (result.success) {
        setIsSuccess(true);
        const amountVndPaid = result.data.amount * 25000;
        setPaidThisTime(amountVndPaid);
        const savedBookingStr = sessionStorage.getItem('currentBooking');
        if (savedBookingStr) {
          setBooking(JSON.parse(savedBookingStr));
        } else {
          setBooking({
            bookingReference: 'BOOK-PAYPAL',
            roomTypeName: 'Hạng Phòng Đã Đặt',
            checkInDate: 'N/A',
            checkOutDate: 'N/A',
            nights: 0,
            finalAmount: amountVndPaid,
            checkInMethod: 'FaceID'
          });
        }
        showToast(t('Thanh toán PayPal thành công!'), 'success');
        sessionStorage.removeItem('currentBooking');
        window.history.replaceState({}, document.title, window.location.pathname);
      } else {
        showToast(t(result.message, t('Lỗi khi capture đơn hàng PayPal')), 'error');
        const savedBookingStr = sessionStorage.getItem('currentBooking');
        if (savedBookingStr) setBooking(JSON.parse(savedBookingStr));
      }
    } catch (err) {
      showToast(t('Lỗi kết nối khi capture thanh toán PayPal'), 'error');
      const savedBookingStr = sessionStorage.getItem('currentBooking');
      if (savedBookingStr) setBooking(JSON.parse(savedBookingStr));
    } finally {
      setIsProcessing(false);
    }
  };

  const handlePaymentSubmit = async (e) => {
    e.preventDefault();
    
    setIsProcessing(true);
    try {
      const idempotencyKey = `create-${Date.now()}`;
      const response = await axiosInstance.post('/payments/paypal/create-order', {
        bookingId: booking.bookingId || booking.id || 1,
        amount: parseFloat(payAmountVnd),
        paymentOption: paidAmountVnd > 0 ? 'FULL' : (payOption === 'deposit' ? 'DEPOSIT' : 'FULL')
      }, {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      });
 
      const result = response.data;
      if (result.success && result.data.approveUrl) {
        window.location.href = result.data.approveUrl;
      } else {
        showToast(t(result.message, t('Không thể tạo đơn hàng PayPal.')), 'error');
        setIsProcessing(false);
      }
    } catch (err) {
      showToast(t('Lỗi kết nối đến server.'), 'error');
      setIsProcessing(false);
    }
  };

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

  const totalAmountVnd = booking.finalAmount || (booking.totalAmount * 1.1) || 0;
  const paidAmountVnd = booking.paidAmount || 0;
  const remainingAmountVnd = totalAmountVnd - paidAmountVnd;

  // If already paid some amount, they pay the remaining. Otherwise, pay option determines the amount.
  const payAmountVnd = paidAmountVnd > 0 
    ? remainingAmountVnd 
    : (payOption === 'deposit' ? totalAmountVnd * 0.3 : totalAmountVnd);



  const checkInMethodText = (method) => {
    switch(method) {
      case 'FaceID': return 'FaceID eKYC';
      case 'QR Code': return t('booking_checkin_qr_option', 'Mã QR');
      default: return t('booking_checkin_manual_option', 'Quầy lễ tân');
    }
  };

  const checkInInstruction = (method) => {
    switch(method) {
      case 'FaceID':
        return t('payment_instruction_faceid', 'Bạn đã chọn nhận phòng FaceID. Vui lòng thiết lập hồ sơ eKYC (Face Embedding) trong trang cá nhân Dashboard để nhận phòng tự động bằng khuôn mặt khi đến khách sạn.');
      case 'QR Code':
        return t('payment_instruction_qrcode', 'Bạn đã chọn nhận phòng bằng QR Code. Mã nhận phòng QR Code đã được tạo và lưu trong Dashboard. Vui lòng xuất trình mã QR Code tại ki-ốt tự động khi nhận phòng.');
      default:
        return t('payment_instruction_manual', 'Vui lòng xuất trình giấy tờ tùy thân tại quầy lễ tân Elysian Hotels để làm thủ tục nhận phòng trực tiếp.');
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
            <h3 className="text-sm font-black uppercase tracking-[0.2em] mb-2 text-primary">{t('payment_processing_overlay', 'Đang xác thực giao dịch')}</h3>
            <p className="text-[10px] text-white/60 font-medium uppercase tracking-widest">{t('payment_processing_overlay_sub', 'Vui lòng không tắt hoặc tải lại trang này...')}</p>
          </div>
        )}

        {isSuccess ? (
          /* Premium Receipt Screen */
          <div className="max-w-2xl w-full bg-white border border-outline-variant shadow-2xl p-8 md:p-12 text-center animate-scale-in">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
              <span className="material-symbols-outlined text-primary text-3xl font-bold">check_circle</span>
            </div>
            <h2 className="font-headline-lg text-headline-md text-primary uppercase italic tracking-wider m-0 mb-2">
              {t('payment_success_receipt_title', 'THANH TOÁN THÀNH CÔNG!')}
            </h2>
            <p className="text-secondary text-[11px] font-bold uppercase tracking-widest border-b border-gray-100 pb-6 mb-8">
              {t('payment_success_receipt_desc', 'Cảm ơn bạn đã lựa chọn Elysian Hotels làm điểm đến của hành trình')}
            </p>

            {/* Receipt Box */}
            <div className="bg-slate-50 border border-slate-200 text-left p-6 md:p-8 space-y-4 mb-8">
              <div className="flex justify-between border-b border-dashed border-slate-300 pb-3">
                <span className="text-[10px] text-slate-400 font-black uppercase tracking-wider">{t('payment_invoice_booking_code', 'Mã đặt phòng (Ref)')}</span>
                <span className="text-xs font-black text-slate-900 uppercase tracking-widest">{booking.bookingReference}</span>
              </div>

              <div className="space-y-2 text-xs font-bold text-slate-700">
                <div className="flex justify-between">
                  <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_room_type', 'Hạng phòng:')}</span>
                  <span className="uppercase">{booking.roomTypeName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_duration', 'Thời gian:')}</span>
                  <span>{booking.checkInDate} {t('booking_summary_date_to', 'đến')} {booking.checkOutDate} ({booking.nights} {t('booking_summary_nights', 'đêm')})</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_checkin_method', 'Phương thức nhận:')}</span>
                  <span className="text-primary uppercase">{checkInMethodText(booking.checkInMethod)}</span>
                </div>
                <div className="flex justify-between border-t border-dashed border-slate-300 pt-3 text-sm">
                  <span className="font-black text-slate-900 uppercase tracking-wider">Số tiền vừa thanh toán:</span>
                  <span className="font-black text-primary text-base">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(paidThisTime)}
                  </span>
                </div>
              </div>

              {/* Check-in Guidance */}
              <div className="bg-primary/5 border-l-2 border-primary p-4 text-[10px] text-slate-700 font-medium leading-relaxed mt-4">
                <span className="block font-black text-primary uppercase tracking-widest mb-1">{t('payment_instruction_title', 'Hướng dẫn nhận phòng:')}</span>
                {checkInInstruction(booking.checkInMethod)}
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button
                onClick={() => setActivePage('dashboard')}
                className="bg-primary text-on-primary font-bold px-8 py-3.5 text-xs uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 h-12"
              >
                <span className="material-symbols-outlined text-sm">dashboard</span> {t('payment_btn_view_dashboard', 'Xem quản lý đặt phòng')}
              </button>
              <button
                onClick={() => setActivePage('home')}
                className="bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold px-8 py-3.5 text-xs uppercase tracking-widest active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 h-12"
              >
                <span className="material-symbols-outlined text-sm">home</span> {t('payment_btn_back_home', 'Trở về trang chủ')}
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
                <span className="material-symbols-outlined text-sm">arrow_back</span> {t('payment_btn_back_edit', 'Quay lại chỉnh sửa')}
              </button>

              <h2 className="font-headline-lg text-headline-md text-primary uppercase italic tracking-wider m-0 mb-2">
                {t('payment_title', 'THANH TOÁN ĐẶT PHÒNG')}
              </h2>
              <p className="text-secondary text-xs font-bold uppercase tracking-widest border-b border-gray-100 pb-4 mb-6">
                {t('payment_subtitle', 'Lựa chọn phương thức thanh toán an toàn để hoàn tất đặt phòng')}
              </p>

              {/* Payment Method Details */}
              <div className="mb-6 pb-2 border-b border-gray-100 flex items-center gap-2">
                <span className="text-xs font-extrabold uppercase tracking-widest text-slate-700">Phương thức thanh toán:</span>
                <span className="px-3 py-1 bg-blue-50 text-blue-700 border border-blue-200 text-xs font-black tracking-widest uppercase">PayPal</span>
              </div>

              {/* Payment Option Selection (Deposit 30% vs Full 100%) */}
              {paidAmountVnd > 0 ? (
                <div className="bg-primary/5 border-l-2 border-primary p-4 mb-6 text-xs text-slate-700 font-bold uppercase tracking-wider">
                  Bạn đã đặt cọc thành công: <span className="text-primary font-black">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(paidAmountVnd)}</span>. 
                  Tiến hành thanh toán nốt số tiền còn lại: <span className="text-primary font-black">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(remainingAmountVnd)}</span>.
                </div>
              ) : (
                <div className="bg-slate-50 border border-slate-200 p-4 mb-6 text-left">
                  <span className="block text-xs font-black text-slate-900 uppercase tracking-widest mb-3">Lựa chọn hình thức thanh toán</span>
                  <div className="grid grid-cols-2 gap-4">
                    <div 
                      onClick={() => setPayOption('deposit')}
                      className={`border p-4 flex flex-col justify-between cursor-pointer transition-all duration-350 ${
                        payOption === 'deposit' 
                          ? 'border-primary bg-primary/5 text-primary shadow-sm' 
                          : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="text-[12px] font-black uppercase tracking-wider block">Đặt cọc trước 30%</span>
                      <span className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mt-1 block">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmountVnd * 0.3)}
                      </span>
                    </div>
                    <div 
                      onClick={() => setPayOption('full')}
                      className={`border p-4 flex flex-col justify-between cursor-pointer transition-all duration-350 ${
                        payOption === 'full' 
                          ? 'border-primary bg-primary/5 text-primary shadow-sm' 
                          : 'border-slate-200 hover:border-primary/50 text-slate-700 bg-white'
                      }`}
                    >
                      <span className="text-[12px] font-black uppercase tracking-wider block">Thanh toán 100%</span>
                      <span className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mt-1 block">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmountVnd)}
                      </span>
                    </div>
                  </div>
                </div>
              )}

              {/* Payment Form Submission */}
              <form onSubmit={handlePaymentSubmit} className="space-y-6">
                <div className="space-y-4 bg-slate-50 border border-slate-200 p-6 text-slate-700 text-center">
                  <div className="w-16 h-16 bg-blue-50/50 border border-blue-200 rounded-full flex items-center justify-center mx-auto mb-2">
                    <svg className="w-8 h-8 text-blue-600 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path d="M20.007 6.78c-.288-1.547-1.332-2.73-3.003-3.414C15.82 2.9 14.183 2.72 12.235 2.72H6.284a1.002 1.002 0 0 0-.985.845L2.518 21.055a.5.5 0 0 0 .493.576h4.556a.5.5 0 0 0 .492-.424l1.378-8.72a1 1 0 0 1 .986-.844h2.247c3.155 0 5.67-1.282 6.398-4.832.336-1.637.159-2.922-.72-3.83z"></path>
                    </svg>
                  </div>
                  <div>
                    <p className="text-xs font-black text-slate-900 uppercase tracking-widest mb-1">Thanh toán qua cổng PayPal</p>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Hệ thống sẽ chuyển hướng bạn sang trang thanh toán bảo mật của PayPal (Sandbox).</p>
                  </div>
                </div>

                {/* Submit button */}
                <button 
                  type="submit"
                  className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-2 h-12"
                >
                  THANH TOÁN QUA PAYPAL ({new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(payAmountVnd)})
                </button>
              </form>
            </div>

            {/* Right Column: Invoice Details (col-span-5) */}
            <div className="lg:col-span-5 bg-white border border-outline-variant shadow-lg p-8 flex flex-col justify-between h-fit">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest mb-4 border-b border-gray-100 pb-2">
                  {t('payment_invoice_title', 'Chi Tiết Hóa Đơn')}
                </h3>

                <div className="space-y-4">
                  {/* Summary Details */}
                  <div className="space-y-3.5 border-b border-gray-100 pb-4 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_booking_code', 'Mã Booking:')}</span>
                      <span className="uppercase tracking-widest">{booking.bookingReference}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_room_type', 'Hạng phòng:')}</span>
                      <span className="uppercase">{booking.roomTypeName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_checkin_date', 'Ngày Nhận phòng:')}</span>
                      <span>{booking.checkInDate}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_checkout_date', 'Ngày Trả phòng:')}</span>
                      <span>{booking.checkOutDate}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_checkin_method', 'Phương thức nhận:')}</span>
                      <span>{checkInMethodText(booking.checkInMethod)}</span>
                    </div>
                  </div>

                  {/* Payment totals */}
                  <div className="space-y-2 pt-2 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Tổng giá trị đặt phòng:</span>
                      <span>
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmountVnd)}
                      </span>
                    </div>
                    {paidAmountVnd > 0 && (
                      <>
                        <div className="flex justify-between text-green-600">
                          <span className="font-bold uppercase tracking-wider">Đã đặt cọc / thanh toán:</span>
                          <span>
                            -{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(paidAmountVnd)}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-400 font-bold uppercase tracking-wider">Số tiền còn lại:</span>
                          <span>
                            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(remainingAmountVnd)}
                          </span>
                        </div>
                      </>
                    )}
                    {paidAmountVnd === 0 && (
                      <div className="flex justify-between">
                        <span className="text-slate-400 font-bold uppercase tracking-wider">Lựa chọn thanh toán:</span>
                        <span className="text-primary font-black uppercase tracking-wider">
                          {payOption === 'deposit' ? 'Đặt cọc 30%' : 'Thanh toán 100%'}
                        </span>
                      </div>
                    )}
                    <div className="flex justify-between border-t border-slate-900 pt-3 text-sm">
                      <span className="font-black text-slate-900 uppercase tracking-wider">TIỀN THANH TOÁN KỲ NÀY:</span>
                      <span className="font-black text-primary text-base">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(payAmountVnd)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Safety notice */}
              <div className="mt-8 border border-slate-150 p-4 text-[9px] text-slate-500 font-semibold leading-relaxed uppercase tracking-wider flex items-start gap-2">
                <span className="material-symbols-outlined text-base text-slate-400">security</span>
                <span>{t('payment_security_notice', 'Thông tin thanh toán của bạn được bảo mật tuyệt đối bởi tiêu chuẩn mã hóa dữ liệu PCI DSS tối cao.')}</span>
              </div>
            </div>
            
          </div>
        )}
      </div>
    </>
  );
}
