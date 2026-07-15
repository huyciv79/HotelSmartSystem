import React from 'react';
import { useLanguage } from '../context/LanguageContext';

export default function HotelPolicy() {
  const { language } = useLanguage();

  const content = {
    VN: {
      title: 'CHÍNH SÁCH BẢO MẬT',
      subtitle: 'Cam kết bảo mật thông tin & quản lý dữ liệu sinh trắc học tại Elysian Smart Hotel Cần Thơ.',
      lastUpdated: 'Cập nhật lần cuối: 15 tháng 07, 2026',
      sections: [
        {
          icon: 'shield',
          heading: '1. Cam Kết Chung',
          text: 'Tại Elysian Smart Hotel, chúng tôi đặt quyền riêng tư và an toàn thông tin của quý khách lên hàng đầu. Chính sách bảo mật này mô tả cách chúng tôi thu thập, sử dụng, bảo vệ và xử lý thông tin cá nhân của bạn khi bạn sử dụng hệ thống đặt phòng, trợ lý ảo AI và các dịch vụ lưu trú thông minh của chúng tôi.'
        },
        {
          icon: 'face',
          heading: '2. Bảo Mật Dữ Liệu Sinh Trắc Học (FaceID & eKYC)',
          text: 'Hệ thống của chúng tôi tích hợp giải pháp check-in thông minh bằng khuôn mặt (eKYC) giúp bạn nhận phòng nhanh chóng không cần thẻ vật lý. Chúng tôi cam kết:',
          bullets: [
            'Chỉ thu thập dữ liệu ảnh khuôn mặt khi bạn chủ động đăng ký trong phần Xác thực danh tính (eKYC).',
            'Dữ liệu khuôn mặt được mã hóa thành các vector đặc trưng (face embeddings) và được lưu trữ an toàn trên máy chủ đám mây riêng biệt của chúng tôi, không lưu trữ ảnh gốc nếu không cần thiết.',
            'Chúng tôi tuyệt đối KHÔNG chia sẻ dữ liệu sinh trắc học của bạn cho bất kỳ bên thứ ba nào vì mục đích thương mại.',
            'Quý khách có toàn quyền xóa bỏ hoàn toàn dữ liệu khuôn mặt của mình bất kỳ lúc nào thông qua trang quản lý tài khoản (Profile/Dashboard).'
          ]
        },
        {
          icon: 'chat',
          heading: '3. Nhật Ký Trò Chuyện Trợ Lý Ảo (AI Concierge Logs)',
          text: 'Trợ lý ảo Elysian AI hỗ trợ bạn tìm phòng, đặt phòng và giải đáp thắc mắc 24/7. Thông tin trò chuyện được xử lý như sau:',
          bullets: [
            'Nội dung tin nhắn trò chuyện được lưu trữ tạm thời trong bộ nhớ trình duyệt (localStorage) của bạn để duy trì mạch hội thoại.',
            'Bạn có thể xóa toàn bộ lịch sử trò chuyện và trạng thái đặt phòng bất cứ lúc nào bằng nút "Restart" ở trang trợ lý ảo hoặc bong bóng chat.',
            'Các câu hỏi và thông tin phi cá nhân có thể được phân tích tự động để huấn luyện và nâng cao chất lượng phản hồi của AI.'
          ]
        },
        {
          icon: 'payments',
          heading: '4. Thông Tin Đặt Phòng & Thanh Toán',
          text: 'Chúng tôi chỉ thu thập các thông tin cần thiết để hoàn tất việc đặt phòng (Tên, Email, SĐT, ngày nhận/trả phòng, số khách).',
          bullets: [
            'Hệ thống thanh toán của chúng tôi được kết nối trực tiếp với cổng thanh toán PayPal.',
            'Elysian Smart Hotel KHÔNG lưu trữ và không truy cập vào thông tin thẻ tín dụng hay tài khoản ngân hàng của bạn. Mọi giao dịch được bảo mật theo tiêu chuẩn quốc tế của PayPal.'
          ]
        },
        {
          icon: 'security',
          heading: '5. Quyền Lợi & Kiểm Soát của Khách Hàng',
          text: 'Quý khách có toàn quyền kiểm soát thông tin cá nhân của mình:',
          bullets: [
            'Truy cập và kiểm tra lịch sử đặt phòng qua Dashboard cá nhân.',
            'Chỉnh sửa thông tin liên hệ và hình ảnh đại diện bất kỳ lúc nào qua trang cá nhân.',
            'Yêu cầu hỗ trợ kỹ thuật hoặc xóa tài khoản vĩnh viễn thông qua cổng liên hệ.'
          ]
        }
      ]
    },
    EN: {
      title: 'PRIVACY POLICY',
      subtitle: 'Commitment to data security & biometric data management at Elysian Smart Hotel Can Tho.',
      lastUpdated: 'Last Updated: July 15, 2026',
      sections: [
        {
          icon: 'shield',
          heading: '1. General Commitment',
          text: 'At Elysian Smart Hotel, we prioritize your privacy and data security. This privacy policy describes how we collect, use, protect, and process your personal information when you use our booking system, AI Concierge, and smart hospitality services.'
        },
        {
          icon: 'face',
          heading: '2. Biometric Data Privacy (FaceID & eKYC)',
          text: 'Our system integrates facial recognition (eKYC) check-in solutions for keyless, rapid check-in at Kiosks. We commit to:',
          bullets: [
            'Only collecting facial photos when you actively register in the Identity Verification (eKYC) section.',
            'Facial data is encrypted into vector characteristics (face embeddings) and stored securely on our private cloud server; raw photos are not kept unless necessary.',
            'We strictly DO NOT share your biometric data with any third party for commercial purposes.',
            'You have the full right to delete your facial biometric data at any time via your Profile/Dashboard.'
          ]
        },
        {
          icon: 'chat',
          heading: '3. AI Concierge Conversation Logs',
          text: 'The Elysian AI Concierge assists you with room search, bookings, and queries 24/7. Conversation data is processed as follows:',
          bullets: [
            'Message contents are stored temporarily in your browser storage (localStorage) to maintain chat context.',
            'You can clear your entire chat history and booking state at any time using the "Restart" button in the assistant page or floating chat bubble.',
            'Non-personal inquiries may be analyzed automatically to train and improve the AI\'s response quality.'
          ]
        },
        {
          icon: 'payments',
          heading: '4. Booking & Payment Details',
          text: 'We only collect essential details to complete your reservation (Name, Email, Phone, dates of stay, number of guests).',
          bullets: [
            'Our system connects directly to the secure PayPal payment gateway.',
            'Elysian Smart Hotel DOES NOT store or access your credit card details or bank credentials. All transactions are protected under PayPal’s global security standards.'
          ]
        },
        {
          icon: 'security',
          heading: '5. Guest Rights & Controls',
          text: 'You maintain full control over your personal information:',
          bullets: [
            'Access and review your booking history through your personal Dashboard.',
            'Edit contact info and profile avatar anytime via your Profile section.',
            'Request technical support or permanent account deletion through our contact portal.'
          ]
        }
      ]
    },
    JP: {
      title: 'プライバシーポリシー',
      subtitle: 'エリ시안 스마트 호텔 카운터에서의 개인 정보 보호 및 생체 정보 관리 안내.',
      lastUpdated: '最終更新日：2026年7月15日',
      sections: [
        {
          icon: 'shield',
          heading: '1. 基本方針',
          text: 'エリシアンスマートホテルでは、お客様のプライバシーとデータセキュリティを第一に考えております。本ポリシーは、予約システム、AIコンシェルジュ、スマート宿泊サービスをご利用の際にお客様の個人情報をどのように収集、使用、保護、処理するかを説明するものです。'
        },
        {
          icon: 'face',
          heading: '2. 生体データの保護 (顔認証およびeKYC)',
          text: '当ホテルでは、キオスクで迅速にチェックインできるよう、顔認証（eKYC）技術を導入しています。私たちは以下を約束します。',
          bullets: [
            'お客様が本人確認（eKYC）セクションで自发的に登録した場合にのみ、顔写真を収集します。',
            '顔データ is encrypted into vector characteristics (face embeddings) and stored securely on our private cloud server; raw photos are not kept unless necessary.',
            '生体データを商業目的で第三者に提供することは一切ありません。',
            'プロフィールまたはダッシュボードから、いつでもご自身の生体データを完全に削除することができます。'
          ]
        },
        {
          icon: 'chat',
          heading: '3. AIアシスタントのチャット履歴について',
          text: 'Elysian AI アシスタントは、24時間年中無休でお客様の客室検索やご予約をサポートします。チャットデータは以下のように処理されます。',
          bullets: [
            '会話の流れを維持するため、メッセージ内容はブラウザのストレージ（localStorage）に一時的に保存されます。',
            'アシスタントページやチャットバブル内の「Restart（再起動）」ボタンを使用して、いつでもすべての履歴を消去できます。',
            '個人を特定しない質問内容は、AIの精度向上のための学習に使用される場合があります。'
          ]
        },
        {
          icon: 'payments',
          heading: '4. ご予約およびお支払い情報',
          text: 'ご予約の完了に必要な最低限の情報（お名前、メールアドレス、電話番号、宿泊日、ゲスト数）のみを収集します。',
          bullets: [
            '決済処理はセキュリティで保護されたPayPalゲートウェイを直接利用しています。',
            '当ホテルはお客様のクレジットカード情報や銀行口座情報を一切保存およびアクセスしません。すべての取引はPayPalの国際的なセキュリティ基準に準拠しています。'
          ]
        },
        {
          icon: 'security',
          heading: '5. お客様の権利とコントロール',
          text: 'お客様はご自身の個人情報を完全に管理する権利を有します。',
          bullets: [
            'マイページダッシュボードからいつでも予約履歴の確認が可能です。',
            'プロフィール編集画面からいつでも連絡先やアバターの変更が可能です。',
            'カスタマーサポートからいつでもアカウントの削除やデータ消去の依頼が可能です。'
          ]
        }
      ]
    },
    KR: {
      title: '개인정보 처리방침',
      subtitle: '엘리시안 스마트 호텔 끈터의 개인정보 보호 및 생체 인식 데이터 관리 안내.',
      lastUpdated: '최종 수정일: 2026년 7월 15일',
      sections: [
        {
          icon: 'shield',
          heading: '1. 개인정보 보호 개요',
          text: '엘리시안 스마트 호텔은 고객님의 개인정보와 데이터 보안을 최우선으로 생각합니다. 본 방침은 당사의 예약 시스템, AI 컨시어지 및 스마트 호텔 서비스를 이용할 때 고객님의 개인정보가 어떻게 수집, 사용, 보호 및 처리되는지 명시합니다.'
        },
        {
          icon: 'face',
          heading: '2. 생체 데이터 프라이버시 (안면인식 및 eKYC)',
          text: '저희 호텔은 키오스크에서 스마트한 빠른 체크인을 제공하기 위해 안면인식(eKYC) 시스템을 갖추고 있습니다. 당사는 다음을 약속드립니다.',
          bullets: [
            '고객님이 신원 인증(eKYC) 시 직접 동의하고 등록한 경우에만 안면 사진을 수집합니다.',
            '수집된 안면 정보는 벡터 데이터로 암호화되어 안전한 독립 클라우드 서버에 보관되며, 원본 이미지는 필요하지 않는 한 저장되지 않습니다.',
            '고객님의 생체인식 데이터를 광고 등 상업적 목적으로 제3자에게 절대 제공하지 않습니다.',
            '고객님은 언제든지 프로필/대시보드를 통해 본인의 안면 생체 데이터를 완전히 삭제할 수 있습니다.'
          ]
        },
        {
          icon: 'chat',
          heading: '3. AI 컨시어지 대화 로그 방침',
          text: 'Elysian AI 비서는 객실 찾기, 예약 진행 및 문의 답변을 24/7 지원합니다. 대화 내용은 다음과 같이 관리됩니다.',
          bullets: [
            '자연스러운 대화 흐름을 위해 대화 내용은 고객님의 브라우저 저장소(localStorage)에 임시 저장됩니다.',
            '채팅방이나 비서 페이지 상단의 "Restart" 버튼을 눌러 언제든지 대화 내역 및 예약 상태를 초기화할 수 있습니다.',
            '개인정보가 제외된 일반적인 문의 내용은 AI 답변 품질 개선을 위해 학습용으로 분석될 수 있습니다.'
          ]
        },
        {
          icon: 'payments',
          heading: '4. 예약 및 결제 정보 보호',
          text: '객실 예약 완료에 필수적인 정보(성함, 이메일, 연락처, 투숙 일자, 인원수)만 수집합니다.',
          bullets: [
            '저희 결제 프로세스는 안전한 글로벌 페이팔(PayPal) 게이트웨이로 연동되어 처리됩니다.',
            '엘리시안 스마트 호텔은 고객님의 신용카드 번호나 비밀번호 등 금융 정보를 절대 저장하거나 접근하지 않습니다.'
          ]
        },
        {
          icon: 'security',
          heading: '5. 고객의 권리 및 통제권',
          text: '고객님은 본인의 개인정보에 대해 완전한 통제권을 가집니다.',
          bullets: [
            '개인 대시보드를 통해 언제든지 모든 예약 내역을 조회할 수 있습니다.',
            '프로필 화면을 통해 연락처 및 프로필 사진을 언제든지 수정할 수 있습니다.',
            '고객 문의를 통해 계정 탈퇴 및 데이터 삭제 요청을 하실 수 있습니다.'
          ]
        }
      ]
    },
    CN: {
      title: '隐私政策',
      subtitle: '极乐智能酒店芹苴店关于保护客户隐私与人脸识别数据管理的承诺。',
      lastUpdated: '最近更新：2026年7月15日',
      sections: [
        {
          icon: 'shield',
          heading: '1. 一般承诺',
          text: '在极乐智能酒店，我们深知个人信息安全对您的重要性。本隐私政策旨在向您说明在您使用我们的在线预订、AI助手和智能入住服务时，我们如何收集、使用、保护和储存您的个人数据。'
        },
        {
          icon: 'face',
          heading: '2. 生体特征识别数据隐私 (人脸识别与eKYC)',
          text: '我们提供基于人脸识别（eKYC）的自助快捷入住。对此，我们向您郑重承诺：',
          bullets: [
            '仅在您主动在“实名认证(eKYC)”页面录入时收集您的面部图像。',
            '面部数据被转换为向量特征并保存在我们安全的云端服务器中。',
            '我们绝不会将您的面部生物特征数据出售或分享给任何第三方用于商业目的。',
            '您有权随时在“个人中心/控制台”删除您的面部识别数据。'
          ]
        },
        {
          icon: 'chat',
          heading: '3. AI虚拟助理聊天日志',
          text: '极乐AI助理24/7协助您查找房间并解答疑问。相关聊天数据处理如下：',
          bullets: [
            '聊天记录和预订状态会保存在您的本地浏览器（localStorage）中，以方便持续对话。',
            '您可以随时点击助理页面或悬浮聊天窗顶部的“Restart”重置按钮，来清空所有本地聊天数据和预订状态。',
            '去标识化的非个人问题可能会用于AI模型的优化和训练，以提升回答质量。'
          ]
        },
        {
          icon: 'payments',
          heading: '4. 预订与支付信息',
          text: '我们仅收集完成预订所必须的信息（姓名、电子邮箱、手机号、入住离店日期、房客数）。',
          bullets: [
            '预订支付过程均由全球安全的PayPal网关处理。',
            '极乐酒店不会储存且无法访问您的信用卡号、CVV安全码或银行账户。所有的交易安全性均由PayPal技术提供保障。'
          ]
        },
        {
          icon: 'security',
          heading: '5. 客户权利与控制',
          text: '您对您的个人信息拥有完全的控制权：',
          bullets: [
            '可通过个人控制台（Dashboard）查询您的全部预订历史。',
            '可随时在“账户设置”里更新您的联系方式和头像。',
            '如有需要，可向客服部门申请彻底注销账户并清除所有相关历史数据。'
          ]
        }
      ]
    }
  };

  const activeContent = content[language] || content.EN;

  return (
    <div className="w-full pt-24 min-h-screen bg-slate-50 flex flex-col font-['Montserrat']">
      {/* Header Banner */}
      <div className="bg-neutral-900 py-10 px-4 md:px-margin-desktop text-left text-white border-b border-primary relative overflow-hidden">
        <div className="absolute right-0 top-0 bottom-0 w-1/3 elysian-pattern opacity-15 hidden md:block" />
        <div className="max-w-7xl mx-auto relative z-10">
          <span className="text-[10px] font-black tracking-[0.25em] text-primary block mb-2 uppercase">
            Elysian Hotels & Resorts
          </span>
          <h1 className="text-3xl font-black tracking-wider uppercase mb-2">
            {activeContent.title}
          </h1>
          <p className="text-xs text-neutral-400 font-bold uppercase tracking-wider">
            {activeContent.subtitle}
          </p>
        </div>
      </div>

      {/* Content Section */}
      <main className="max-w-4xl mx-auto w-full px-4 md:px-6 py-12 flex-grow text-left">
        <div className="bg-white border border-slate-200 p-8 md:p-12 shadow-sm rounded-none">
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-8">
            {activeContent.lastUpdated}
          </span>

          <div className="space-y-10">
            {activeContent.sections.map((section, idx) => (
              <div key={idx} className="space-y-4">
                <h3 className="text-base font-black text-slate-900 uppercase tracking-widest flex items-center gap-2.5 border-b border-slate-100 pb-3">
                  <span className="material-symbols-outlined text-primary text-xl">
                    {section.icon}
                  </span>
                  {section.heading}
                </h3>
                
                <p className="text-[13.5px] text-slate-600 font-medium leading-relaxed">
                  {section.text}
                </p>

                {section.bullets && (
                  <ul className="space-y-3.5 pl-6 list-none p-0 m-0">
                    {section.bullets.map((bullet, i) => (
                      <li key={i} className="text-[13px] text-slate-600 font-medium leading-relaxed flex items-start gap-3">
                        <span className="material-symbols-outlined text-primary text-[16px] shrink-0 mt-0.5">
                          check_circle
                        </span>
                        <span>{bullet}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}
