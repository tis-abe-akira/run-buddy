/**
 * run-buddy's own mark: a coral pace dot on a looping route line.
 * (DESIGN.md's spike-mark is Anthropic's logo and is deliberately not used.)
 */
export function Wordmark() {
  return (
    <>
      <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden="true">
        <path
          d="M3 16c3-9 8-11 11-8s-2 7 1 8 4-3 4-6"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        />
        <circle cx="3.5" cy="16" r="3" fill="var(--color-primary)" />
      </svg>
      <span className="wordmark-text">run-buddy</span>
    </>
  )
}
