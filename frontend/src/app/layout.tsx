import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'SecureGate — Multi-Tenant Identity & Access Management',
  description: 'Enterprise-grade multi-tenant authentication, authorization, passkeys, and tamper-evident audit logging for SaaS.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full bg-slate-50">
      <body className="h-full text-slate-900 antialiased">{children}</body>
    </html>
  );
}
