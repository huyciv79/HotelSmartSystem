import { Component } from 'react';
import { AlertOctagon, RefreshCw, Home } from 'lucide-react';

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Uncaught error caught by ErrorBoundary:', error, errorInfo);
    this.setState({ errorInfo });
  }

  handleReload = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#0d0d0f] flex items-center justify-center p-6 font-['Montserrat'] text-slate-100">
          <div className="max-w-md w-full bg-[#141416] border border-neutral-800 p-8 shadow-2xl flex flex-col items-center text-center gap-6">
            <div className="w-16 h-16 bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-500">
              <AlertOctagon size={32} />
            </div>

            <div className="space-y-2">
              <h2 className="text-lg font-black uppercase tracking-wider text-white">
                ĐÃ XẢY RA LỖI HỆ THỐNG
              </h2>
              <p className="text-xs text-neutral-400 font-medium leading-relaxed uppercase tracking-wider">
                Ứng dụng gặp sự cố ngoài dự kiến trong quá trình xử lý. Chúng tôi đã ghi nhận lỗi này.
              </p>
            </div>

            {this.state.error?.message && (
              <div className="w-full bg-black/40 border border-neutral-800 p-3 text-[10px] text-red-400 font-mono text-left overflow-x-auto max-h-24">
                {this.state.error.toString()}
              </div>
            )}

            <div className="flex gap-3 w-full pt-2">
              <button
                onClick={this.handleReload}
                className="flex-1 py-3 bg-primary hover:bg-white hover:text-black text-white font-black uppercase text-[10px] tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-2 parallelogram-btn"
              >
                <RefreshCw size={14} />
                <span>Thử lại</span>
              </button>

              <button
                onClick={this.handleGoHome}
                className="flex-1 py-3 bg-neutral-800 hover:bg-neutral-700 text-white font-black uppercase text-[10px] tracking-widest transition-all cursor-pointer border border-neutral-700 flex items-center justify-center gap-2"
              >
                <Home size={14} />
                <span>Trang chủ</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
