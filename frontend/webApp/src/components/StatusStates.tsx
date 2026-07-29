import type { ReactNode } from "react";

export const LoadingState = ({ label = "Loading..." }: { label?: string }) => (
  <p className="status-message" role="status">{label}</p>
);
export const EmptyState = ({ children }: { children: ReactNode }) => (
  <p className="empty-message" role="status">{children}</p>
);
export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <div className="error-message" role="alert"><p>{message}</p>{onRetry && <button onClick={onRetry}>Try again</button>}</div>;
}
