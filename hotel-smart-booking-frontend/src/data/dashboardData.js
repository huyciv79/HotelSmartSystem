import {
  CalendarDays,
  CheckCircle,
  Clock,
  CreditCard,
  Gift,
  UtensilsCrossed,
  Car,
  Sparkles,
  CreditCard as PaymentIcon,
} from 'lucide-react';

export const USER_PROFILE = {
  name: 'Alexander',
  tier: 'DIAMOND ELITE',
  nextStayDate: 'November 14, 2024',
  avatarUrl: 'https://i.pravatar.cc/36?img=12',
};

export const STATS = [
  {
    id: 'upcoming',
    icon: CalendarDays,
    value: '02',
    label: 'Upcoming Stays',
    badge: '+12%',
    badgeColor: 'text-green-500',
    accentColor: 'text-amber-600',
    accentBg: 'bg-amber-200/10',
    hasBorderTop: true,
  },
  {
    id: 'completed',
    icon: CheckCircle,
    value: '48',
    label: 'Completed Stays',
    badge: '—',
    badgeColor: 'text-zinc-700/40',
    accentColor: 'text-zinc-900',
    accentBg: 'bg-slate-900/5',
    hasBorderTop: false,
  },
  {
    id: 'pending',
    icon: Clock,
    value: '05',
    label: 'Pending Requests',
    badge: '3 Active',
    badgeColor: 'text-amber-500',
    accentColor: 'text-amber-600',
    accentBg: 'bg-amber-500/10',
    hasBorderTop: false,
  },
  {
    id: 'spending',
    icon: CreditCard,
    value: '$12,450',
    label: 'Total Spending',
    badge: '+2.4k',
    badgeColor: 'text-green-500',
    accentColor: 'text-zinc-900',
    accentBg: 'bg-slate-900/5',
    hasBorderTop: false,
  },
];

export const CURRENT_BOOKING = {
  suiteName: 'Grand Deluxe Suite',
  refCode: 'EK-88294410',
  status: 'Confirmed',
  checkIn: {
    date: 'Nov 14',
    detail: 'Thursday, 2:00 PM',
  },
  checkOut: {
    date: 'Nov 19',
    detail: 'Tuesday, 11:00 AM',
  },
  guests: {
    count: '02 Adults',
    detail: '1 King Bed',
  },
};

export const BOOKING_HISTORY = [
  {
    id: 1,
    destination: 'Kenther Zurich',
    roomType: 'Presidential Wing',
    period: 'Oct 12 - Oct 15, 2024',
    amount: '$4,200',
    status: 'Completed',
  },
  {
    id: 2,
    destination: 'Kenther Kyoto',
    roomType: 'Zen Garden Villa',
    period: 'Aug 20 - Aug 25, 2024',
    amount: '$6,800',
    status: 'Completed',
  },
  {
    id: 3,
    destination: 'Kenther London',
    roomType: 'Executive Suite',
    period: 'May 01 - May 03, 2024',
    amount: '$1,450',
    status: 'Completed',
  },
];

export const CONCIERGE_REQUESTS = [
  {
    id: 1,
    title: 'Dinner Reservation',
    description: "Requested for 'Lumière' @ 8:30 PM, Oct 20",
    status: 'PENDING APPROVAL',
    statusColor: 'text-amber-500',
    dotColor: 'bg-amber-200',
    isActive: true,
  },
  {
    id: 2,
    title: 'Private Car Transfer',
    description: 'Arrival from Narita Airport, Oct 14',
    status: 'CONFIRMED',
    statusColor: 'text-green-500',
    dotColor: 'bg-black',
    isActive: false,
  },
  {
    id: 3,
    title: 'Spa Treatment',
    description: "Couple's Aromatherapy, Oct 16",
    status: 'CONFIRMED',
    statusColor: 'text-green-500',
    dotColor: 'bg-black',
    isActive: false,
  },
];

export const UPDATES = [
  {
    id: 1,
    icon: Gift,
    iconBg: 'bg-amber-200/20',
    iconColor: 'text-amber-200',
    title: 'Anniversary Offer',
    preview: 'Celebrate your 5th year with us. Enjoy a complimentary bottle of…',
    time: '2 hours ago',
  },
  {
    id: 2,
    icon: UtensilsCrossed,
    iconBg: 'bg-slate-900/10',
    iconColor: 'text-black',
    title: 'New Autumn Menu',
    preview: 'Our Michelin star kitchen has released the new tasting menu…',
    time: 'Yesterday',
  },
  {
    id: 3,
    icon: PaymentIcon,
    iconBg: 'bg-green-500/10',
    iconColor: 'text-green-500',
    title: 'Payment Processed',
    preview: 'Your deposit for the Kyoto villa has been successfully confirmed.',
    time: '2 days ago',
  },
];

export const PAYMENT_OVERVIEW = {
  paidAmount: '$12,450',
  pendingBalance: '$4,200',
  bars: [
    { height: '100%', color: 'bg-black' },
    { height: '37.5%', color: 'bg-amber-200' },
    { height: '9.4%', color: 'bg-black/20' },
    { height: '15.6%', color: 'bg-black/20' },
    { height: '4.7%', color: 'bg-black/20' },
  ],
};

export const RECOMMENDED_SUITES = [
  {
    id: 1,
    name: 'Skyline Penthouse',
    amenities: '120m² • Terrace • Butler',
    price: '$2,400',
    rating: '4.9',
    imageUrl: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&h=250&fit=crop',
  },
  {
    id: 2,
    name: 'Royal Spa Suite',
    amenities: '85m² • Private Spa • Garden',
    price: '$1,850',
    rating: '5.0',
    imageUrl: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400&h=250&fit=crop',
  },
  {
    id: 3,
    name: 'Heritage Library Room',
    amenities: '65m² • Library • Fireplace',
    price: '$950',
    rating: '4.8',
    imageUrl: 'https://images.unsplash.com/photo-1590490360182-c33d955c4644?w=400&h=250&fit=crop',
  },
];

export const SIDEBAR_NAV_ITEMS = [
  { id: 'overview', label: 'Overview', isActive: true },
  { id: 'stays', label: 'My Stays', isActive: false },
  { id: 'preferences', label: 'Preferences', isActive: false },
  { id: 'rewards', label: 'Rewards', isActive: false },
  { id: 'messages', label: 'Messages', isActive: false },
  { id: 'support', label: 'Support', isActive: false },
];
