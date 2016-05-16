class Vector {
   std::vector<double> k;
   int N;
public:
   explicit Vector(int size): k(size) {     
      N = size;
      for (int i = 0; i < N; i++)
         k[i] = 0.0;
   }
  
   inline int GetSize() const { return N; }
  
   inline double& operator[] (int ix) {
      CHECK(ix < N && ix >= 0);
      return k[ix];
   }
  
   inline const double& operator[] (int ix) const {
      CHECK(ix < N && ix >= 0);
      return k[ix];
   }
  
   inline Vector operator+ (const Vector & other) const {
      CHECK(other.GetSize() == N);
      Vector ret(N);
      for (int i = 0; i < N; i++)
         ret[i] = k[i] + other[i];
      return ret;
   }
  
   inline Vector operator- (const Vector & other) const {
      CHECK(other.GetSize() == N);
      Vector ret(N);
      for (int i = 0; i < N; i++)
         ret[i] = k[i] - other[i];
      return ret;
   }

   inline Vector operator* (double factor) const {
      Vector ret(N);
      for (int i = 0; i < N; i++)
         ret[i] = k[i] * factor;
      return ret;
   }
  
   inline double DotProduct (const Vector & other) const {
      CHECK(other.GetSize() == N);
      double ret = 0.0;
      for (int i = 0; i < N; i++)
         ret += k[i] * other[i];
      return ret;
   }
  
   inline double Len2 () const {
      double ret = 0.0;
      for (int i = 0; i < N; i++)
         ret += k[i] * k[i];
      return ret;     
   }
  
   inline double Len () const { return sqrt(Len2()); }
  
   string ToString () const {
      char temp[1024] = "(";
      for (int i = 0; i < N; i++)
         sprintf(temp, "%s%s%.1lf", temp, i == 0 ? "" : ", ", k[i]);
      return (std::string)temp + ")";
   }
  
   inline void Copy(const Vector & from) {
      CHECK(from.GetSize() == N);
      for (int i = 0; i < N; i++)
         k[i] = from[i];
   }
};

class Matrix {
   int M;
   std::vector<double*> data;
public:
   /*
    * This matrix can grow its "N" i.e. width
    * See http://en.wikipedia.org/wiki/Matrix_(mathematics)
    */
  
   Matrix (int M_, int N) {
      M = M_;
      for (int i = 0; i < N; i++) {
         IncN();
      }
   }
  
   ~Matrix() {
      for (int i = 0; i < data.size(); i++)
         delete [] data[i];
   }

   inline int GetM() const { return M; }
  
   inline int GetN() const { return data.size(); }
   inline void IncN() {
      double * k = new double[M];
      for (int i = 0; i < M; i++)
         k[i] = 0.0;
      data.push_back(k);
   }
  
  
   inline double& At(int i, int j) {
      CHECK(i < M && i >= 0);
      CHECK(j < data.size() && j >= 0);
      // Note the reverse order of indices!
      return data[j][i];
   }
  
   inline const double& At(int i, int j) const {
      CHECK(i < M && i >= 0);
      CHECK(j < data.size() && j >= 0);
      // Note the reverse order of indices!
      return data[j][i];
   }
  
   Vector MultiplyRight (const Vector & v) const {
      int N = data.size();
      CHECK(v.GetSize() == N);
      Vector ret(M);
      for (int i = 0; i < M; i++)
         for (int j = 0; j < N; j++)
            ret[i] += v[j] * At(i,j);
      return ret;
   }
  
   Vector MultiplyLeft (const Vector & v_to_transpose) const {
      int N = data.size();
      CHECK(v_to_transpose.GetSize() == M);
      Vector ret(N);
      for (int i = 0; i < M; i++)
         for (int j = 0; j < N; j++)
            ret[j] += v_to_transpose[i] * At(i,j);
      return ret;     
   }
  
   string ToString() const {
      string ret = "";
      for (int i = 0; i < M; i++) {
         ret += "[";
         for (int j = 0; j < GetN(); j++) {
            char temp[128] = "";
            sprintf(temp, "%s%.1lf", j == 0 ? "" : ", ", At(i,j));
            ret += temp;
         }
         ret += "]\n";
      }
      return ret;
   }
};

Vector EstimateParameters(const Matrix & perf_m, const Vector & stats_v, double rel_diff, int * iter_count = NULL)
{
   /*
    *  Goal: find Vector<N> parameters:
    *   (perf_m * parameters - stats_v)^2 -> min
    * With the following limitations:
    *    parameters[i] >= 0
    * "Stop rule":
    *    for every i=0...M
    *    -> |stats_v[i] - (perf_m * parameters)[i]| < rel_diff * stats_v[i]
    * Algorithm used is a gradient descend
    */
   int N = perf_m.GetN(), M = perf_m.GetM();
   CHECK(stats_v.GetSize() == M);
   Vector current(N);

   // First, let's find out those lines in matrix having only one non-zero coefficient
   // and if we have any, decrease the dimension of our matrix
   {
      int count_easy_param = 0;
      std::vector<int> parameters_set(N); // N (line#) -> M (row#) of the only nonzero element; -1 if 0/2+
      for (int n = 0; n < N; n++) {
         parameters_set[n] = -1;
      }
      // we may detect & assert unresolvable conflicts like the following
      // |1|       |1|   
      // |1| * p = |2|
      // |1|       |3|

      // Find out those 0000*0000 lines
      for (int m = 0; m < M; m++) {
         int zero_id = -1; // -1 = not found, -2 = found twice, else - idx
         for (int n = 0; n < N; n++) {
            const double & m_n = perf_m.At(m,n);
            if (m_n != 0.0) {
               if (zero_id == -1) {
                  zero_id = n;
               } else if (zero_id >= 0) {
                  zero_id = -2;
                  break;
               }
            }
         }
         if (zero_id >= 0) {
            CHECK(parameters_set[zero_id] == -1);
            parameters_set[zero_id] = m;
            count_easy_param++;
            current[zero_id] = stats_v[m] / perf_m.At(m, zero_id);
         }
      }

      if (count_easy_param > 0) {
         //printf("tmp = %s\n", current.ToString().c_str());
 
         //printf("\n\nDecreasing the dimensions!\n");
         std::vector<int> new_m_to_old(M - count_easy_param),
                          new_n_to_old(N - count_easy_param);
         for (int m = 0; m < M - count_easy_param; m++) {
            // see increments later
            new_m_to_old[m] = m;
         }
         int cur_n = 0;
         for (int n = 0; n < N; n++) {
            if (parameters_set[n] == -1) {
               new_n_to_old[cur_n] = n;
               cur_n++;
            } else {
               for (int m = parameters_set[n]; m < M - count_easy_param; m++) {
                  new_m_to_old[m]++;
               }
            }
         }

         Vector auto_stats = perf_m.MultiplyRight(current), // we have these stats for sure ...
                diff_stats = stats_v - auto_stats; // ... and we need to get these stats more
     
         // Formulate the sub-problem (sub-matrix & sub-stats)
         Matrix new_m(M - count_easy_param, N - count_easy_param);
         Vector new_stats(M - count_easy_param);
         for (int m = 0; m < M - count_easy_param; m++) {
            new_stats[m] = diff_stats[new_m_to_old[m]];
            CHECK(new_stats[m] >= 0.0);
            for (int n = 0; n < N - count_easy_param; n++)
               new_m.At(m,n) = perf_m.At(new_m_to_old[m], new_n_to_old[n]);
         }
        
         // Solve the submatrix and put the results back
         Vector new_param = EstimateParameters(new_m, new_stats, rel_diff, iter_count);
         for (int n = 0; n < N - count_easy_param; n++) {
            current[new_n_to_old[n]] = new_param[n];
         }
         return current;
      }
   }

   bool stop_condition = false;
   double prev_distance = stats_v.Len2();

   //printf("Initial distance = %.1lf\n", prev_distance);
   while (stop_condition == false) {
      (*iter_count)++;
      Vector m_by_curr(M);
      m_by_curr.Copy(perf_m.MultiplyRight(current));
      Vector derivative(N); // this is actually "-derivative/2" :-)
      derivative.Copy(perf_m.MultiplyLeft(stats_v - m_by_curr));

      if (derivative.Len() > 1000.0) {
         derivative = derivative * (1000.0 / derivative.Len());
      }

      // Descend according to the gradient
      {
         Vector new_curr(N);
         double step = 1024.0;
         double new_distance;
         for (int i = 0; i < N; i++) {
            if (current[i] + derivative[i] * step < 0.0) {
               step = - current[i] / derivative[i];
            }
         }
         do {
            new_curr = current + derivative*step;
            step /= 2.0;
            new_distance = (perf_m.MultiplyRight(new_curr) - stats_v).Len2();
            if (step < 0.00001) {
               stop_condition = true;
            }
            (*iter_count)++;
         } while (new_distance >= prev_distance && !stop_condition);
        
         prev_distance = new_distance;
         current = new_curr;
         //printf ("Dist=%.1lf, cur=%s\n", new_distance, current.ToString().c_str());
      }
     
      // Check stop condition
      if (stop_condition == false)
      {
         stop_condition = true;
         Vector stats_est(M);
         stats_est.Copy(perf_m.MultiplyRight(current));
         for (int i = 0; i < M; i++)
            if (fabs(stats_v[i] - stats_est[i]) > rel_diff * stats_v[i])
               stop_condition = false;
      }
   }
  
   return current;
}
