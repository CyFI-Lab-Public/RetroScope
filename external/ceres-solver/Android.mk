# Ceres Solver - A fast non-linear least squares minimizer
# Copyright 2010, 2011, 2012 Google Inc. All rights reserved.
# http://code.google.com/p/ceres-solver/
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice,
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Google Inc. nor the names of its contributors may be
#   used to endorse or promote products derived from this software without
#   specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: settinger@google.com (Scott Ettinger)
#         keir@google.com (Keir Mierle)
#
# Builds Ceres for Android, using the standard toolchain (not standalone). It
# uses STLPort instead of GNU C++. This is useful for anyone wishing to ship
# GPL-free code. This cannot build the tests or other parts of Ceres; only the
# core libraries. If you need a more complete Ceres build, consider using the
# CMake toolchain (noting that the standalone toolchain doesn't work with
# STLPort).
#
# Reducing binary size:
#
# This build includes the Schur specializations, which cause binary bloat. If
# you don't need them for your application, consider adding:
#
#   -DCERES_RESTRICT_SCHUR_SPECIALIZATION
#
# to the LOCAL_CFLAGS variable below, and commenting out all the
# generated/schur_eliminator_2_2_2.cc-alike files, leaving only the _d_d_d one.
#
# Similarly if you do not need the line search minimizer, consider adding
#
#   -DCERES_NO_LINE_SEARCH_MINIMIZER

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libceres

LOCAL_SDK_VERSION := 17
LOCAL_NDK_STL_VARIANT := stlport_static

LOCAL_C_INCLUDES := $(LOCAL_PATH)/internal \
                    $(LOCAL_PATH)/internal/ceres \
                    $(LOCAL_PATH)/internal/ceres/miniglog \
                    $(LOCAL_PATH)/include \
                    external/eigen \

LOCAL_CPP_EXTENSION := .cc
LOCAL_CPPFLAGS := -DCERES_NO_PROTOCOL_BUFFERS \
                  -DCERES_NO_LAPACK \
                  -DCERES_NO_SUITESPARSE \
                  -DCERES_NO_GFLAGS \
                  -DCERES_NO_THREADS \
                  -DCERES_NO_CXSPARSE \
                  -DCERES_NO_TR1 \
                  -DCERES_WORK_AROUND_ANDROID_NDK_COMPILER_BUG \
                  -DMAX_LOG_LEVEL=-1 \
                  -O3 -w

# On Android NDK 8b, GCC gives spurrious warnings about ABI incompatibility for
# which there is no solution. Hide the warning instead.
LOCAL_CFLAGS += -Wno-psabi

LOCAL_SRC_FILES := internal/ceres/array_utils.cc \
                   internal/ceres/blas.cc \
                   internal/ceres/block_evaluate_preparer.cc \
                   internal/ceres/block_jacobian_writer.cc \
                   internal/ceres/block_jacobi_preconditioner.cc \
                   internal/ceres/block_random_access_dense_matrix.cc \
                   internal/ceres/block_random_access_matrix.cc \
                   internal/ceres/block_random_access_sparse_matrix.cc \
                   internal/ceres/block_sparse_matrix.cc \
                   internal/ceres/block_structure.cc \
                   internal/ceres/canonical_views_clustering.cc \
                   internal/ceres/cgnr_solver.cc \
                   internal/ceres/compressed_row_jacobian_writer.cc \
                   internal/ceres/compressed_row_sparse_matrix.cc \
                   internal/ceres/conditioned_cost_function.cc \
                   internal/ceres/conjugate_gradients_solver.cc \
                   internal/ceres/coordinate_descent_minimizer.cc \
                   internal/ceres/corrector.cc \
                   internal/ceres/dense_normal_cholesky_solver.cc \
                   internal/ceres/dense_qr_solver.cc \
                   internal/ceres/dense_sparse_matrix.cc \
                   internal/ceres/detect_structure.cc \
                   internal/ceres/dogleg_strategy.cc \
                   internal/ceres/evaluator.cc \
                   internal/ceres/file.cc \
                   internal/ceres/gradient_checking_cost_function.cc \
                   internal/ceres/implicit_schur_complement.cc \
                   internal/ceres/iterative_schur_complement_solver.cc \
                   internal/ceres/lapack.cc \
                   internal/ceres/levenberg_marquardt_strategy.cc \
                   internal/ceres/line_search.cc \
                   internal/ceres/line_search_direction.cc \
                   internal/ceres/line_search_minimizer.cc \
                   internal/ceres/linear_least_squares_problems.cc \
                   internal/ceres/linear_operator.cc \
                   internal/ceres/linear_solver.cc \
                   internal/ceres/local_parameterization.cc \
                   internal/ceres/loss_function.cc \
                   internal/ceres/low_rank_inverse_hessian.cc \
                   internal/ceres/minimizer.cc \
                   internal/ceres/normal_prior.cc \
                   internal/ceres/parameter_block_ordering.cc \
                   internal/ceres/partitioned_matrix_view.cc \
                   internal/ceres/polynomial.cc \
                   internal/ceres/preconditioner.cc \
                   internal/ceres/problem.cc \
                   internal/ceres/problem_impl.cc \
                   internal/ceres/program.cc \
                   internal/ceres/residual_block.cc \
                   internal/ceres/residual_block_utils.cc \
                   internal/ceres/runtime_numeric_diff_cost_function.cc \
                   internal/ceres/schur_complement_solver.cc \
                   internal/ceres/schur_eliminator.cc \
                   internal/ceres/schur_jacobi_preconditioner.cc \
                   internal/ceres/scratch_evaluate_preparer.cc \
                   internal/ceres/solver.cc \
                   internal/ceres/solver_impl.cc \
                   internal/ceres/sparse_matrix.cc \
                   internal/ceres/sparse_normal_cholesky_solver.cc \
                   internal/ceres/split.cc \
                   internal/ceres/stringprintf.cc \
                   internal/ceres/suitesparse.cc \
                   internal/ceres/triplet_sparse_matrix.cc \
                   internal/ceres/trust_region_minimizer.cc \
                   internal/ceres/trust_region_strategy.cc \
                   internal/ceres/types.cc \
                   internal/ceres/visibility_based_preconditioner.cc \
                   internal/ceres/visibility.cc \
                   internal/ceres/wall_time.cc \
                   internal/ceres/generated/schur_eliminator_d_d_d.cc \
                   internal/ceres/generated/schur_eliminator_2_2_2.cc \
                   internal/ceres/generated/schur_eliminator_2_2_3.cc \
                   internal/ceres/generated/schur_eliminator_2_2_4.cc \
                   internal/ceres/generated/schur_eliminator_2_2_d.cc \
                   internal/ceres/generated/schur_eliminator_2_3_3.cc \
                   internal/ceres/generated/schur_eliminator_2_3_4.cc \
                   internal/ceres/generated/schur_eliminator_2_3_9.cc \
                   internal/ceres/generated/schur_eliminator_2_3_d.cc \
                   internal/ceres/generated/schur_eliminator_2_4_3.cc \
                   internal/ceres/generated/schur_eliminator_2_4_4.cc \
                   internal/ceres/generated/schur_eliminator_2_4_d.cc \
                   internal/ceres/generated/schur_eliminator_4_4_2.cc \
                   internal/ceres/generated/schur_eliminator_4_4_3.cc \
                   internal/ceres/generated/schur_eliminator_4_4_4.cc \
                   internal/ceres/generated/schur_eliminator_4_4_d.cc

include $(BUILD_STATIC_LIBRARY)
