// Type traits for google3's custom MathUtil traits class. This is needed to
// enable embedding Jet objects inside the Quaternion class, found in
// util/math/quaternion.h. Including this file makes it possible to use
// quaternions inside Ceres cost functions which are automatically
// differentiated; for example:
//
//   struct MyCostFunction {
//     template<T>
//     bool Map(const T* const quaternion_parameters, T* residuals) {
//       Quaternion<T> quaternion(quaternion_parameters);
//       ...
//     }
//   }
//
// NOTE(keir): This header must be included before quaternion.h or other
// file relying on traits. Adding a direct dependency on this header from
// mathlimits.h is a bad idea, so it is up to clients to use the correct include
// order.

#ifndef JET_TRAITS_H
#define JET_TRAITS_H

#include "ceres/jet.h"
#include "util/math/mathlimits.h"

template<typename T, int N>
struct MathLimits<ceres::Jet<T, N> > {
  typedef ceres::Jet<T, N> Type;
  typedef ceres::Jet<T, N> UnsignedType;
  static const bool kIsSigned = true;
  static const bool kIsInteger = false;
  static const Type kPosMin;
  static const Type kPosMax;
  static const Type kMin;
  static const Type kMax;
  static const Type kNegMin;
  static const Type kNegMax;
  static const int  kMin10Exp;
  static const int  kMax10Exp;
  static const Type kEpsilon;
  static const Type kStdError;
  static const int  kPrecisionDigits;
  static const Type kNaN;
  static const Type kPosInf;
  static const Type kNegInf;
  static bool IsFinite(const Type x) { return isfinite(x); }
  static bool IsNaN   (const Type x) { return isnan(x);    }
  static bool IsInf   (const Type x) { return isinf(x);    }
  static bool IsPosInf(const Type x) {
    bool found_inf = MathLimits<T>::IsPosInf(x.a);
    for (int i = 0; i < N && !found_inf; ++i) {
      found_inf = MathLimits<T>::IsPosInf(x.v[i]);
    }
    return found_inf;
  }
  static bool IsNegInf(const Type x) {
    bool found_inf = MathLimits<T>::IsNegInf(x.a);
    for (int i = 0; i < N && !found_inf; ++i) {
      found_inf = MathLimits<T>::IsNegInf(x.v[i]);
    }
    return found_inf;
  }
};

// Since every one of these items is a simple forward to the scalar type
// underlying the jet, use a tablular format which makes the structure clear.
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kPosMin          = ceres::Jet<T, N>(MathLimits<T>::kPosMin);    // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kPosMax          = ceres::Jet<T, N>(MathLimits<T>::kPosMax);    // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kMin             = ceres::Jet<T, N>(MathLimits<T>::kMin);       // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kMax             = ceres::Jet<T, N>(MathLimits<T>::kMax);       // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kNegMin          = ceres::Jet<T, N>(MathLimits<T>::kNegMin);    // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kNegMax          = ceres::Jet<T, N>(MathLimits<T>::kNegMax);    // NOLINT
template<typename T, int N> const int              MathLimits<ceres::Jet<T, N> >::kMin10Exp        = MathLimits<T>::kMin10Exp;                    // NOLINT
template<typename T, int N> const int              MathLimits<ceres::Jet<T, N> >::kMax10Exp        = MathLimits<T>::kMax10Exp;                    // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kEpsilon         = ceres::Jet<T, N>(MathLimits<T>::kEpsilon);   // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kStdError        = ceres::Jet<T, N>(MathLimits<T>::kStdError);  // NOLINT
template<typename T, int N> const int              MathLimits<ceres::Jet<T, N> >::kPrecisionDigits = MathLimits<T>::kPrecisionDigits;             // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kNaN             = ceres::Jet<T, N>(MathLimits<T>::kNaN);       // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kPosInf          = ceres::Jet<T, N>(MathLimits<T>::kPosInf);    // NOLINT
template<typename T, int N> const ceres::Jet<T, N> MathLimits<ceres::Jet<T, N> >::kNegInf          = ceres::Jet<T, N>(MathLimits<T>::kNegInf);    // NOLINT

#endif  // JET_TRAITS_H
