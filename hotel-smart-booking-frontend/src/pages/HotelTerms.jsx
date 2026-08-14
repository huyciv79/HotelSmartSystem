import React from 'react';
import { useLanguage } from '../context/LanguageContext';

export default function HotelTerms() {
  const { language } = useLanguage();

  const content = {
    VN: {
      title: 'ĐIỀU KHOẢN DỊCH VỤ',
      subtitle: 'Quy định, chính sách đặt phòng và lưu trú tại The Iris Smart Hotel Cần Thơ.',
      lastUpdated: 'Cập nhật lần cuối: 15 tháng 07, 2026',
      sections: [
        {
          icon: 'gavel',
          heading: '1. Quy Định Đặt Phòng & Nhận Phòng',
          text: 'Để đảm bảo trải nghiệm lưu trú tốt nhất, quý khách vui lòng tuân thủ quy trình đặt phòng và nhận/trả phòng sau:',
          bullets: [
            'Giờ nhận phòng (Check-in) tiêu chuẩn là từ 14:00 và giờ trả phòng (Check-out) là trước 12:00 trưa hàng ngày.',
            'Quý khách nhận phòng thông minh (Kiosk FaceID hoặc Mã QR) bắt buộc phải hoàn tất xác thực danh tính điện tử (eKYC) trước trên trang cá nhân.',
            'Yêu cầu đặt phòng nhóm (Group Booking từ 2 phòng trở lên) sẽ được áp dụng chính sách ưu đãi riêng biệt theo quy định của hệ thống.'
          ]
        },
        {
          icon: 'payments',
          heading: '2. Thanh Toán & Đặt Cọc',
          text: 'Chúng tôi hỗ trợ phương thức thanh toán trực tuyến bảo mật và tiện lợi:',
          bullets: [
            'Hệ thống hiện tại chấp nhận thanh toán trực tuyến qua cổng PayPal.',
            'Yêu cầu thanh toán 100% giá trị đặt phòng để xác nhận đơn phòng thành công.',
            'Mọi hóa đơn chi tiết (Invoice) bao gồm tiền phòng và tiền dịch vụ phát sinh sẽ được cung cấp đầy đủ thông qua Dashboard cá nhân hoặc Trợ lý ảo AI.'
          ]
        },
        {
          icon: 'published_with_changes',
          heading: '3. Thay Đổi, Gia Hạn & Hủy Phòng',
          text: 'The Iris cung cấp sự linh hoạt tối đa cho kỳ nghỉ của bạn thông qua các tính năng tự phục vụ trên hệ thống:',
          bullets: [
            '**Hủy đặt phòng:** Quý khách có thể yêu cầu hủy phòng thông qua Trợ lý ảo AI. Số tiền hoàn lại (nếu có) sẽ phụ thuộc vào chính sách hủy của từng hạng phòng cụ thể.',
            '**Đổi hạng phòng:** Yêu cầu đổi phòng có thể được thực hiện khi đang lưu trú. Lệ phí phát sinh hoặc chênh lệch giá phòng sẽ được tính toán tự động và hiển thị cho quý khách xác nhận trước khi gửi lễ tân duyệt.',
            '**Gia hạn lưu trú:** Việc gia hạn ngày trả phòng phải được gửi trước ít nhất 24 giờ so với giờ check-out dự kiến và tùy thuộc vào tình trạng phòng trống tại thời điểm đó.'
          ]
        },
        {
          icon: 'room_service',
          heading: '4. Nội Quy Lưu Trú',
          text: 'Để duy trì môi trường nghỉ dưỡng văn minh và an toàn, quý khách vui lòng lưu ý:',
          bullets: [
            'Không hút thuốc lá (bao gồm cả thuốc lá điện tử) trong phòng nghỉ. Vui lòng sử dụng khu vực hút thuốc được chỉ định.',
            'Không mang vật nuôi, chất dễ cháy nổ, vũ khí hoặc các chất cấm vào khuôn viên khách sạn.',
            'Bảo quản các thiết bị thông minh và nội thất được trang bị trong phòng. Mọi hư hại do cố ý sẽ phải bồi thường theo giá trị tài sản.'
          ]
        },
        {
          icon: 'info',
          heading: '5. Miễn Trừ Trách Nhiệm & Tranh Chấp',
          text: 'Các điều khoản bổ sung về trách nhiệm pháp lý:',
          bullets: [
            'The Iris Smart Hotel không chịu trách nhiệm đối với các mất mát tài sản cá nhân không được ký gửi trong két an toàn hoặc quầy lễ tân.',
            'Mọi tranh chấp phát sinh từ hoặc liên quan đến thỏa thuận đặt phòng này trước hết sẽ được giải quyết thông qua thương lượng thiện chí giữa hai bên.'
          ]
        }
      ]
    },
    EN: {
      title: 'TERMS OF SERVICE',
      subtitle: 'Rules, booking policies, and stay regulations at The Iris Smart Hotel Can Tho.',
      lastUpdated: 'Last Updated: July 15, 2026',
      sections: [
        {
          icon: 'gavel',
          heading: '1. Booking & Check-in Regulations',
          text: 'To ensure a seamless stay, please observe the following check-in/out and reservation procedures:',
          bullets: [
            'Standard check-in time is from 14:00, and check-out time is before 12:00 noon daily.',
            'Guests opting for smart check-in (FaceID Kiosk or QR Code) must complete their electronic identity verification (eKYC) on their profile beforehand.',
            'Group bookings (2 or more rooms) are subject to distinct group policies and discounted rates as calculated by our system.'
          ]
        },
        {
          icon: 'payments',
          heading: '2. Payment & Deposit',
          text: 'We support secure and convenient online payment methods:',
          bullets: [
            'The system currently processes online payments via the PayPal gateway.',
            'A 100% pre-payment of the booking value is required to successfully confirm your reservation.',
            'Detailed invoices containing room rates and any auxiliary service charges can be accessed through your Dashboard or requested via the AI Assistant.'
          ]
        },
        {
          icon: 'published_with_changes',
          heading: '3. Booking Changes, Extensions & Cancellations',
          text: 'The Iris offers maximum flexibility for your stay through self-service options:',
          bullets: [
            '**Cancellations:** Booking cancellations can be requested via the AI Concierge. Refund eligibility depends on the specific cancel policy of your selected room type.',
            '**Room Changes:** Room adjustment requests can be submitted during your stay. Extra charges or room price differences will be automatically calculated and displayed for your approval.',
            '**Stay Extensions:** Request for extensions must be submitted at least 24 hours prior to standard check-out time, subject to room availability.'
          ]
        },
        {
          icon: 'room_service',
          heading: '4. Hotel House Rules',
          text: 'To maintain a clean and safe environment for all guests, please note:',
          bullets: [
            'Strictly no smoking (including e-cigarettes) inside guest rooms. Designated smoking areas are available.',
            'Pets, hazardous items, weapons, and illegal substances are strictly prohibited on hotel premises.',
            'Please handle smart room amenities and furniture with care. Any deliberate damage will be charged to the guest\'s account.'
          ]
        },
        {
          icon: 'info',
          heading: '5. Disclaimer & Dispute Resolution',
          text: 'Additional terms regarding liability:',
          bullets: [
            'The Iris Smart Hotel is not responsible for personal valuables that are not deposited in the in-room safe or at the front desk.',
            'Any dispute arising out of or in connection with this booking agreement shall first be resolved through friendly negotiations.'
          ]
        }
      ]
    },
    JP: {
      title: '利用規約',
      subtitle: 'エリシアンスマートホテルカントーにおけるご予約、ご宿泊に関する規定とポリシー。',
      lastUpdated: '最終更新日：2026年7月15日',
      sections: [
        {
          icon: 'gavel',
          heading: '1. ご予約およびチェックイン規約',
          text: '快適なご滞在のために、以下のチェックイン・アウトおよび予約手続きをお守りください。',
          bullets: [
            '標準チェックイン時刻は14:00から、チェックアウト時刻は正午12:00までとなります。',
            'スマートチェックイン（顔認証キオスクまたはQRコード）をご希望の場合は、事前に対象のプロフィールで本人確認（eKYC）を完了させておく必要があります。',
            '団体予約（2室以上のご予約）には、当システムによって計算される独自の団体ポリシーおよび割引料金が適用されます。'
          ]
        },
        {
          icon: 'payments',
          heading: '2. お支払いとデポジット',
          text: '安全で便利なオンライン決済方法をサポートしています。',
          bullets: [
            '現在、システムはPayPalゲートウェイ経由のオンライン決済を処理しています。',
            '予約を正常に確定するには、予約金額の100%の前払いが必要となります。',
            '室料やその他の付帯サービス料金を含む詳細な請求書は、ダッシュボードからアクセスするか、AIアシスタントに請求することができます。'
          ]
        },
        {
          icon: 'published_with_changes',
          heading: '3. 予約変更、延滞およびキャンセル',
          text: 'エリシアンは、セルフサービスオプションを通じてご滞在に最大限の柔軟性を提供します。',
          bullets: [
            '**キャンセル：** 予約のキャンセルは、AIコンシェルジュを介してリクエストできます。返金規定は選択された客室タイプのキャンセルポリシーに従います。',
            '**部屋タイプの変更：** 滞在中の部屋変更リクエストを提出できます。追加料金や差額は自動的に計算され、お客様の承認を得るために表示されます。',
            '**滞在期間の延長：** 延長のリクエストは、部屋の空き状況に基づき、標準のチェックアウト時刻の少なくとも24時間前までに提出する必要があります。'
          ]
        },
        {
          icon: 'room_service',
          heading: '4. ハウスルール（館内規則）',
          text: 'すべてのお客様に清潔で安全な環境を維持するため、次の点にご注意ください。',
          bullets: [
            '客室内は電子タバコを含め禁煙です。指定の喫煙エリアをご利用ください。',
            'ペット、危険物、武器、および違法薬物の持ち込みは固くお断りいたします。',
            '客室内のスマートアメニティや家具は丁寧にお取り扱いください。故意の破損は弁償の対象となります。'
          ]
        },
        {
          icon: 'info',
          heading: '5. 免責事項および紛争解決',
          text: '免責事項に関する追加規約：',
          bullets: [
            '客室内の金庫またはフロントにお預けにならなかった貴重品の紛失について、当ホテルは一切の責任を負いません。',
            '本予約契約に起因または関連する紛争は、まず両者間の友好的な交渉を通じて解決されるものとします。'
          ]
        }
      ]
    },
    KR: {
      title: '이용약관',
      subtitle: '엘리시안 스마트 호텔 끈터의 객실 예약 및 투숙에 관한 이용 규정.',
      lastUpdated: '최종 수정일: 2026년 7월 15일',
      sections: [
        {
          icon: 'gavel',
          heading: '1. 예약 및 체크인 규정',
          text: '원활한 투숙을 위해 다음의 체크인/체크아웃 및 예약 절차를 준수해 주시기 바랍니다.',
          bullets: [
            '표준 체크인 시간은 14:00부터이며, 체크아웃 시간은 매일 정오 12:00 이전입니다.',
            '스마트 체크인(안면인식 키오스크 또는 QR코드)을 이용하시는 고객님은 사전에 마이페이지에서 본인 인증(eKYC)을 완료하셔야 합니다.',
            '단체 예약(객실 2개 이상)의 경우 개별적인 단체 요금 및 예약 규정이 적용됩니다.'
          ]
        },
        {
          icon: 'payments',
          heading: '2. 결제 및 보증금 안내',
          text: '안전하고 편리한 온라인 결제 방식을 지원합니다.',
          bullets: [
            '당사의 시스템은 현재 글로벌 페이팔(PayPal) 게이트웨이를 통한 온라인 결제를 처리합니다.',
            '예약을 확정하기 위해서는 총 예약 금액의 100% 결제가 필요합니다.',
            '객실 요금 및 추가 서비스 이용 내역이 포함된 상세 영수증은 대시보드 또는 AI 비서를 통해 조회하실 수 있습니다.'
          ]
        },
        {
          icon: 'published_with_changes',
          heading: '3. 예약 변경, 연장 및 취소',
          text: '엘리시안은 셀프서비스 시스템을 통해 유연한 일정 변경 옵션을 제공합니다.',
          bullets: [
            '**취소 및 환불:** 예약 취소는 AI 컨시어지 비서를 통해 신청할 수 있습니다. 환불 가능 여부는 객실별 환불 취소 규정에 따릅니다.',
            '**방 변경(Room Change):** 투숙 중에 객실 변경을 요청할 수 있으며, 발생하는 추가 요금이나 차액은 시스템에서 자동 계산되어 제시됩니다.',
            '**숙박 연장:** 체크아웃 예정 시간 최소 24시간 전에 신청하셔야 하며, 객실 공실 상황에 따라 제한될 수 있습니다.'
          ]
        },
        {
          icon: 'room_service',
          heading: '4. 객실 이용 수칙',
          text: '쾌적하고 안전한 투숙 환경을 위해 다음 규정을 준수해 주십시오.',
          bullets: [
            '객실 내에서는 전자담배를 포함하여 절대 금연입니다. 흡연은 지정된 구역에서만 가능합니다.',
            '애완동물, 위험 물질, 무기 및 불법 약물의 반입을 금지합니다.',
            '객실 내 스마트 홈 장비 및 가구류를 파손하지 않도록 주의해 주십시오. 고의 파손 시 청구될 수 있습니다.'
          ]
        },
        {
          icon: 'info',
          heading: '5. 책임 제한 및 분쟁 해결',
          text: '법적 책임에 관한 추가 조항:',
          bullets: [
            '객실 금고나 안내 데스크에 보관하지 않은 개인 귀중품의 분실에 대해 당사는 책임을 지지 않습니다.',
            '예약 약관과 관련하여 발생하는 모든 분쟁은 양자 간의 원만한 합의와 협의를 통해 우선적으로 해결합니다.'
          ]
        }
      ]
    },
    CN: {
      title: '服务条款',
      subtitle: '极乐智能酒店芹苴店关于预订、入住及店规的相关条款与政策。',
      lastUpdated: '最近更新：2026年7月15日',
      sections: [
        {
          icon: 'gavel',
          heading: '1. 预订与入住登记规定',
          text: '为保障您的顺利入住，请遵守以下预订与离店流程：',
          bullets: [
            '标准的入住登记时间为14:00起，退房结账时间为每日正午12:00前。',
            '使用智能入住（自助机人脸识别或扫码）的客人在入住前需在个人主页完成电子实名认证（eKYC）。',
            '团体预订（2间及以上客房）需遵守特定的团体政策，系统将自动核算相应的团购价格。'
          ]
        },
        {
          icon: 'payments',
          heading: '2. 付款与账单结算',
          text: '我们提供安全便利的在线付款方式：',
          bullets: [
            '目前在线支付由PayPal安全网关提供支付技术支持。',
            '需支付100%的预定款项后方可为您成功保留并锁定房间。',
            '包含房费及 auxiliary 消费的详细账单均可随时在控制台查询，或通过AI助理生成并发送给您。'
          ]
        },
        {
          icon: 'published_with_changes',
          heading: '3. 订单修改、延住与退房取消',
          text: '极乐智能酒店通过自助系统为您提供灵活的订单处理方式：',
          bullets: [
            '**取消订单：** 您可以通过AI虚拟客服申请退房取消，退款额度将遵循该房型对应的退改政策。',
            '**在店换房：** 入住期间可提交换房申请，差价或手续费将由系统自动计算生成。',
            '**延时住店：** 续住延期需在预定退房时间前至少24小时提交，且视当天客房空置状态而定。'
          ]
        },
        {
          icon: 'room_service',
          heading: '4. 住客守则',
          text: '为营造安全文明的住宿环境，请您注意：',
          bullets: [
            '所有客房内部严禁吸烟（包括电子烟），请使用酒店设立的指定吸烟区。',
            '严禁携带宠物、易燃易爆危险品、武器或各类违禁品进入酒店范围。',
            '请爱护客房内智能家电及家具配置，故意损坏需按资产原价进行赔偿。'
          ]
        },
        {
          icon: 'info',
          heading: '5. 免责条款与争议解决',
          text: '关于法律责任的补充说明：',
          bullets: [
            '客房保险箱外或前台未寄存的个人私人物品发生遗失或损坏，酒店不承担相关赔偿责任。',
            '因本预订协议引起的任何争议，双方应本着友好协商的原则优先进行沟通和解决。'
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
            The Iris Hotel & Resorts
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
