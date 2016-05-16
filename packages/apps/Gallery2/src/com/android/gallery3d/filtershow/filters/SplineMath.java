package com.android.gallery3d.filtershow.filters;


public class SplineMath {
    double[][] mPoints = new double[6][2];
    double[] mDerivatives;
    SplineMath(int n) {
        mPoints = new double[n][2];
    }

    public void setPoint(int index, double x, double y) {
        mPoints[index][0] = x;
        mPoints[index][1] = y;
        mDerivatives = null;
    }

    public float[][] calculatetCurve(int n) {
        float[][] curve = new float[n][2];
        double[][] points = new double[mPoints.length][2];
        for (int i = 0; i < mPoints.length; i++) {

            points[i][0] = mPoints[i][0];
            points[i][1] = mPoints[i][1];

        }
        double[] derivatives = solveSystem(points);
        float start = (float) points[0][0];
        float end = (float) (points[points.length - 1][0]);

        curve[0][0] = (float) (points[0][0]);
        curve[0][1] = (float) (points[0][1]);
        int last = curve.length - 1;
        curve[last][0] = (float) (points[points.length - 1][0]);
        curve[last][1] = (float) (points[points.length - 1][1]);

        for (int i = 0; i < curve.length; i++) {

            double[] cur = null;
            double[] next = null;
            double x = start + i * (end - start) / (curve.length - 1);
            int pivot = 0;
            for (int j = 0; j < points.length - 1; j++) {
                if (x >= points[j][0] && x <= points[j + 1][0]) {
                    pivot = j;
                }
            }
            cur = points[pivot];
            next = points[pivot + 1];
            if (x <= next[0]) {
                double x1 = cur[0];
                double x2 = next[0];
                double y1 = cur[1];
                double y2 = next[1];

                // Use the second derivatives to apply the cubic spline
                // equation:
                double delta = (x2 - x1);
                double delta2 = delta * delta;
                double b = (x - x1) / delta;
                double a = 1 - b;
                double ta = a * y1;
                double tb = b * y2;
                double tc = (a * a * a - a) * derivatives[pivot];
                double td = (b * b * b - b) * derivatives[pivot + 1];
                double y = ta + tb + (delta2 / 6) * (tc + td);

                curve[i][0] = (float) (x);
                curve[i][1] = (float) (y);
            } else {
                curve[i][0] = (float) (next[0]);
                curve[i][1] = (float) (next[1]);
            }
        }
        return curve;
    }

    public double getValue(double x) {
        double[] cur = null;
        double[] next = null;
        if (mDerivatives == null)
            mDerivatives = solveSystem(mPoints);
        int pivot = 0;
        for (int j = 0; j < mPoints.length - 1; j++) {
            pivot = j;
            if (x <= mPoints[j][0]) {
                break;
            }
        }
        cur = mPoints[pivot];
        next = mPoints[pivot + 1];
        double x1 = cur[0];
        double x2 = next[0];
        double y1 = cur[1];
        double y2 = next[1];

        // Use the second derivatives to apply the cubic spline
        // equation:
        double delta = (x2 - x1);
        double delta2 = delta * delta;
        double b = (x - x1) / delta;
        double a = 1 - b;
        double ta = a * y1;
        double tb = b * y2;
        double tc = (a * a * a - a) * mDerivatives[pivot];
        double td = (b * b * b - b) * mDerivatives[pivot + 1];
        double y = ta + tb + (delta2 / 6) * (tc + td);

        return y;

    }

    double[] solveSystem(double[][] points) {
        int n = points.length;
        double[][] system = new double[n][3];
        double[] result = new double[n]; // d
        double[] solution = new double[n]; // returned coefficients
        system[0][1] = 1;
        system[n - 1][1] = 1;
        double d6 = 1.0 / 6.0;
        double d3 = 1.0 / 3.0;

        // let's create a tridiagonal matrix representing the
        // system, and apply the TDMA algorithm to solve it
        // (see http://en.wikipedia.org/wiki/Tridiagonal_matrix_algorithm)
        for (int i = 1; i < n - 1; i++) {
            double deltaPrevX = points[i][0] - points[i - 1][0];
            double deltaX = points[i + 1][0] - points[i - 1][0];
            double deltaNextX = points[i + 1][0] - points[i][0];
            double deltaNextY = points[i + 1][1] - points[i][1];
            double deltaPrevY = points[i][1] - points[i - 1][1];
            system[i][0] = d6 * deltaPrevX; // a_i
            system[i][1] = d3 * deltaX; // b_i
            system[i][2] = d6 * deltaNextX; // c_i
            result[i] = (deltaNextY / deltaNextX) - (deltaPrevY / deltaPrevX); // d_i
        }

        // Forward sweep
        for (int i = 1; i < n; i++) {
            // m = a_i/b_i-1
            double m = system[i][0] / system[i - 1][1];
            // b_i = b_i - m(c_i-1)
            system[i][1] = system[i][1] - m * system[i - 1][2];
            // d_i = d_i - m(d_i-1)
            result[i] = result[i] - m * result[i - 1];
        }

        // Back substitution
        solution[n - 1] = result[n - 1] / system[n - 1][1];
        for (int i = n - 2; i >= 0; --i) {
            solution[i] = (result[i] - system[i][2] * solution[i + 1]) / system[i][1];
        }
        return solution;
    }

    public static void main(String[] args) {
        SplineMath s = new SplineMath(10);
        for (int i = 0; i < 10; i++) {
            s.setPoint(i, i, i);
        }
        float[][] curve = s.calculatetCurve(40);

        for (int j = 0; j < curve.length; j++) {
            System.out.println(curve[j][0] + "," + curve[j][1]);
        }
    }
}
