'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Shield, Mail, Lock, User, Building2, CheckCircle2, AlertCircle, ArrowRight } from 'lucide-react';
import { api } from '@/lib/api';

export default function RegisterPage() {
  const router = useRouter();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [registeredSuccess, setRegisteredSuccess] = useState(false);

  // Password strength checks
  const hasMinLength = password.length >= 8;
  const hasUpper = /[A-Z]/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecial = /[^A-Za-z0-9]/.test(password);

  const passedCount = [hasMinLength, hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length;
  const strengthColor =
    passedCount <= 2 ? 'bg-rose-500' : passedCount <= 4 ? 'bg-amber-500' : 'bg-emerald-500';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (passedCount < 3) {
      setErrorMessage('Please create a stronger password before proceeding.');
      return;
    }

    setErrorMessage(null);
    setLoading(true);

    try {
      await api.fetch('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          fullName,
          email,
          password,
          organizationName,
        }),
      });

      setRegisteredSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.detail || 'Registration failed. An account with this email may already exist.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <Link href="/" className="flex items-center justify-center gap-2 mb-2">
          <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
            <Shield className="w-6 h-6" />
          </div>
          <span className="text-2xl font-bold tracking-tight text-slate-900">SecureGate</span>
        </Link>
        <h2 className="text-center text-xl font-bold tracking-tight text-slate-800">
          Create your SecureGate account
        </h2>
        <p className="mt-1 text-center text-xs text-slate-500">
          Set up your organization tenant with defense-in-depth isolation
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow-sm border border-slate-200 rounded-2xl sm:px-10">
          {registeredSuccess ? (
            <div className="text-center py-6 space-y-4">
              <div className="mx-auto w-12 h-12 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <h3 className="text-base font-bold text-slate-900">Organization Created!</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                We sent an email verification link to <strong className="text-slate-800">{email}</strong>.
                Please verify your email address to activate all security features.
              </p>
              <div className="pt-2">
                <Link href="/login" className="btn-primary w-full text-center">
                  Sign in to Dashboard <ArrowRight className="w-4 h-4 ml-1" />
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {errorMessage && (
                <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{errorMessage}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Full Name
                </label>
                <div className="relative">
                  <User className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Alice Johnson"
                    className="input-field pl-9"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Work Email Address
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="alice@company.com"
                    className="input-field pl-9"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Initial Organization Name
                </label>
                <div className="relative">
                  <Building2 className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                  <input
                    type="text"
                    required
                    value={organizationName}
                    onChange={(e) => setOrganizationName(e.target.value)}
                    placeholder="Acme Cyber Systems"
                    className="input-field pl-9"
                  />
                </div>
                <p className="mt-1 text-[11px] text-slate-400">
                  A unique tenant slug will be created automatically.
                </p>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Password (Argon2id Encrypted)
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••••••"
                    className="input-field pl-9"
                  />
                </div>

                {/* Password strength meter */}
                {password.length > 0 && (
                  <div className="mt-2 space-y-1">
                    <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                      <div
                        className={`h-full ${strengthColor} transition-all duration-300`}
                        style={{ width: `${(passedCount / 5) * 100}%` }}
                      />
                    </div>
                    <div className="flex flex-wrap gap-2 text-[10px] text-slate-500 pt-1">
                      <span className={hasMinLength ? 'text-emerald-600 font-semibold' : ''}>• 8+ chars</span>
                      <span className={hasUpper ? 'text-emerald-600 font-semibold' : ''}>• Uppercase</span>
                      <span className={hasLower ? 'text-emerald-600 font-semibold' : ''}>• Lowercase</span>
                      <span className={hasNumber ? 'text-emerald-600 font-semibold' : ''}>• Number</span>
                      <span className={hasSpecial ? 'text-emerald-600 font-semibold' : ''}>• Symbol</span>
                    </div>
                  </div>
                )}
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn-primary w-full mt-4"
              >
                {loading ? 'Registering Organization...' : 'Create Account & Organization'}
              </button>
            </form>
          )}

          <div className="mt-6 text-center text-xs text-slate-500">
            Already have an account?{' '}
            <Link href="/login" className="font-semibold text-sky-600 hover:underline">
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
