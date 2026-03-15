import React from 'react';

/**
 * Handles profile image URL resolution with fallback strategy:
 * 1. Convert relative paths to full API URLs (e.g., /uploads/profile-pics/9_profile.png → http://localhost:8091/uploads/profile-pics/9_profile.png)
 * 2. Use normal HTTP/HTTPS URLs directly
 * 3. Fall back to first letter of name (if image fails to load or URL is empty/null)
 */

const API_BASE_URL = 'http://localhost:8091';

/**
 * Convert relative path to full API URL with cache busting
 * @param pathOrUrl - The path or URL to convert
 * @returns Full URL with cache-busting timestamp query parameter
 */
export function resolveImageUrl(pathOrUrl: string): string {
  if (!pathOrUrl) return '';
  
  // If it's already a full HTTP/HTTPS URL, return as-is (with cache buster)
  if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
    return addCacheBuster(pathOrUrl);
  }
  
  // If it's a relative path (starts with /), prepend API base URL
  if (pathOrUrl.startsWith('/')) {
    return addCacheBuster(`${API_BASE_URL}${pathOrUrl}`);
  }
  
  // For any other format, return as-is (might be data URL or other formats)
  return pathOrUrl;
}

/**
 * Add cache-busting timestamp to URL to force fresh image fetch
 * @param url - The full URL
 * @returns URL with cache-busting timestamp query parameter
 */
function addCacheBuster(url: string): string {
  const separator = url.includes('?') ? '&' : '?';
  return `${url}${separator}t=${Date.now()}`;
}

/**
 * Check if a URL is a valid HTTP/HTTPS or relative path
 * @param url - The URL or path to check
 * @returns boolean - True if URL looks valid (relative or http/https)
 */
export function isValidImagePath(url: string): boolean {
  if (!url || url.trim() === '') return false;
  
  // Allow relative paths, http, https, and data URLs
  const validStart = url.startsWith('/') || 
                     url.startsWith('http://') || 
                     url.startsWith('https://') || 
                     url.startsWith('data:');
  
  return validStart;
}

/**
 * React hook for profile image with intelligent fallback
 * Returns either an image URL or initials based on availability
 * 
 * Usage:
 * const { imageUrl, initials, shouldShowImage } = useProfileImage(profileUrl, userName);
 * 
 * if (shouldShowImage && imageUrl) {
 *   <img src={imageUrl} onError={() => showInitials()} />
 * } else {
 *   <div>{initials}</div>
 * }
 */
export function useProfileImage(
  profileUrl: string | null | undefined,
  userName: string
): {
  imageUrl: string | null;
  initials: string;
  shouldShowImage: boolean;
} {
  const initials = userName.charAt(0).toUpperCase();

  // If no URL provided, show initials
  if (!profileUrl || profileUrl.trim() === '') {
    return {
      imageUrl: null,
      initials,
      shouldShowImage: false
    };
  }

  // If URL looks like file:// protocol (not allowed by browsers), show initials
  if (profileUrl.startsWith('file://')) {
    console.warn(
      'File protocol URLs are blocked by browser security policy. ' +
      'Use relative paths (e.g., /uploads/profile-pics/...) or HTTP/HTTPS URLs instead.',
      profileUrl
    );
    return {
      imageUrl: null,
      initials,
      shouldShowImage: false
    };
  }

  // If URL is valid format, resolve it and allow it to load (img tag will handle failures)
  if (isValidImagePath(profileUrl)) {
    const resolvedUrl = resolveImageUrl(profileUrl);
    return {
      imageUrl: resolvedUrl,
      initials,
      shouldShowImage: true
    };
  }

  // Fallback to initials for invalid URLs
  return {
    imageUrl: null,
    initials,
    shouldShowImage: false
  };
}

/**
 * Helper component for avatar with fallback
 * Automatically handles image loading failures
 * Supports both naming conventions: userName/profileUrl or name/url
 */
export const ProfileAvatar = React.forwardRef<
  HTMLDivElement,
  {
    profileUrl?: string | null | undefined;
    userName?: string;
    url?: string | null | undefined;
    name?: string;
    size?: number;
    className?: string;
  }
>(({ profileUrl, userName, url, name, size = 32, className = '' }, ref) => {
  // Support both naming conventions: userName/profileUrl or name/url
  const finalUrl = profileUrl ?? url;
  const finalName = userName ?? name ?? 'U';
  const { imageUrl, initials, shouldShowImage } = useProfileImage(finalUrl, finalName);
  const [showInitials, setShowInitials] = React.useState(!shouldShowImage);

  const handleImageError = () => {
    setShowInitials(true);
  };

  // Prevent rendering img tag if imageUrl contains file:// protocol
  const canShowImage = showInitials === false && imageUrl && !imageUrl.startsWith('file://');

  const containerStyle: React.CSSProperties = {
    width: `${size}px`,
    height: `${size}px`,
    minWidth: `${size}px`,
    fontSize: `${size / 2}px`,
    overflow: 'hidden',
    backgroundColor: 'var(--bs-primary)',
    color: 'white',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 'bold',
    borderRadius: className.includes('rounded-circle') ? '50%' : '0.375rem'
  };

  const imgStyle: React.CSSProperties = {
    width: '100%',
    height: '100%',
    objectFit: 'cover'
  };

  return React.createElement(
    'div',
    {
      ref,
      className: `d-flex align-items-center justify-content-center fw-bold rounded bg-primary text-white ${className}`,
      style: containerStyle
    },
    canShowImage
      ? React.createElement('img', {
          src: imageUrl,
          alt: userName,
          onError: handleImageError,
          style: imgStyle
        })
      : initials
  );
});
