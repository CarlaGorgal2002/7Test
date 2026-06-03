export default function Logo({ dark = false, size = 48 }) {
  const fill = dark ? '#ffffff' : '#09222A'
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 85"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <polygon points="50,5 92,27 50,49 8,27" fill={fill} />
      <path d="M22 42 L22 62 Q22 70 50 70 Q78 70 78 62 L78 42 Z" fill={fill} />
      <line x1="85" y1="27" x2="85" y2="58" stroke={fill} strokeWidth="4" strokeLinecap="round" />
      <circle cx="85" cy="64" r="5.5" fill={fill} />
    </svg>
  )
}
