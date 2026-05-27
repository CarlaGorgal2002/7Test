import { useLayoutEffect, useRef } from 'react'

export default function AutoGrowTextarea({
  value = '',
  onChange,
  style,
  minHeight = 180,
  maxHeight = 900,
  ...props
}) {
  const ref = useRef(null)

  useLayoutEffect(() => {
    resize()
  }, [value, minHeight, maxHeight])

  function resize() {
    const element = ref.current
    if (!element) return
    element.style.height = 'auto'
    const nextHeight = Math.max(minHeight, Math.min(element.scrollHeight + 2, maxHeight))
    element.style.height = `${nextHeight}px`
    element.style.overflowY = element.scrollHeight > maxHeight ? 'auto' : 'hidden'
  }

  return (
    <textarea
      ref={ref}
      value={value}
      onChange={(event) => {
        onChange?.(event)
        requestAnimationFrame(resize)
      }}
      style={{ ...style, minHeight, resize: 'none' }}
      {...props}
    />
  )
}
