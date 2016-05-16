/*
  This file is part of Valgrind, a dynamic binary instrumentation
  framework.

  Copyright (C) 2008-2008 Google Inc
     opensource@google.com

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License as
  published by the Free Software Foundation; either version 2 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
  02111-1307, USA.

  The GNU General Public License is contained in the file COPYING.
*/

// Author: Timur Iskhodzhanov <opensource@google.com>
//
// This file contains a set of benchmarks for data race detection tools.

#include <vector>
#include <string>
#include <map>
#include <set>
#include <algorithm>
#include <cstring>      // strlen(), index(), rindex()
#include <ctime>
#include <math.h>

#include "thread_wrappers.h"
#include "linear_solver.h"

class Mutex64: public Mutex {
   // force sizeof(Mutex64) >= 64
private:
   char ___[(sizeof(Mutex) > 64) ? (0) : (64 - sizeof(Mutex))];
};

enum StatType {
   ZZZERO,
   N_THREADS,
   N_CV,
   N_CV_SIGNALS,
   N_CV_WAITS,
   N_MUTEXES,
   N_MUTEX_LOCK_UNLOCK,
   N_MEM_ACCESSES_K, // kiloaccesses
   /*N_RACEY_ACCESSES*/
};

class Test{
   typedef void (*void_func_void_t)(void);
  
   /* may return false to indicate smth like "Wait didn't succeed" */
   typedef bool (*bool_func_void_t)(void);
   //typedef TestStats (*TestStats_func_void_t)(void);
  
   //TestStats_func_void_t GetStats_;
   void_func_void_t Run_v_;
   bool_func_void_t Run_b_;
 public:
   Test() : Run_v_(0), Run_b_(0) {}
   Test(int id, void_func_void_t _Run) : Run_v_(_Run), Run_b_(0) {
     CHECK(Run_v_ != NULL);
   }
   Test(int id, bool_func_void_t _Run) : Run_v_(0), Run_b_(_Run) {
     CHECK(Run_b_ != NULL);
   }
   bool Run() {
      if (Run_v_ == NULL) {
         CHECK(Run_b_ != NULL);
         return (*Run_b_)();
      } else {
         Run_v_();
         return true;
      }
   }
};

typedef std::map<int, Test> MapOfTests;
MapOfTests the_map_of_tests;

class GoalStats {
public:
   typedef std::vector<StatType> TypesVector;
private:
   //TODO: think of better names
   std::map<StatType, int> goal;
   TypesVector types;
   Vector * stats;
   typedef std::set<void (*)()> void_f_void_set;
   void_f_void_set param_registerers, param_appliers;
   std::vector<double*> parameters;
   Matrix * cost_m;
   bool calculated;
  
   template<typename T> static int v_find(const std::vector<T> & vec, const T & val) {
      for (int i = 0; i < vec.size(); i++)
         if (vec[i] == val)
            return i;
      return -1;
   }
public:
   GoalStats(): stats(NULL), cost_m(NULL), calculated(false) {}
   ~GoalStats() { delete stats; delete cost_m; }
  
   // Note that this function is called before main()
   void AddPattern(void (*register_func)(), void (*paramapply_func)()) {
      param_registerers.insert(register_func);
      param_appliers.insert(paramapply_func);
   }  
  
   void RegisterPatterns() {
      for(void_f_void_set::iterator i = param_registerers.begin();
            i != param_registerers.end(); i++)
      {
         (*(*i))(); // call each param_registerer
      }
   }
  
   void AddGoal(StatType type, int value) {
      CHECK(stats == NULL);
      CHECK(goal.find(type) == goal.end());
      goal[type] = value;
      types.push_back(type);
   }
  
   void CompileStatsIntoVector() {
      CHECK(stats == NULL);
      CHECK(types.size() == goal.size());
      stats = new Vector(types.size());
      for (int i = 0; i < types.size(); i++)
         (*stats)[i] = goal[types[i]];
      cost_m = new Matrix(types.size(), 0);
   }
  
   const Vector & GetStatsVector() {
      return *stats;
   }
  
   TypesVector GetTypes() {
      CHECK(stats != NULL);
      return types;
   }
  
   void RegisterParameter(double * param) {
      CHECK(stats != NULL);
      // int param_id = parameters.size();
      parameters.push_back(param);
      cost_m->IncN();
   }
  
   void SetParameterStat(StatType stat_type, double * param, double cost) {
      int type_id = v_find(types, stat_type);
      if (type_id == -1)
         return; // this stat type isn't required - ignore
     
      int param_id = v_find(parameters, param);
      CHECK(param_id != -1);        
     
      cost_m->At(type_id, param_id) = cost;
   }
  
   void CalculateAndApplyParameters() {
      CHECK(calculated == false);
      printf("Cost matrix:\n%s\n", cost_m->ToString().c_str());
      printf("Stats vector:\n%s\n", stats->ToString().c_str());
      int iterations = 0;
      Vector params = EstimateParameters(*cost_m, *stats, 0.0005, &iterations);
      CHECK(params.GetSize() == parameters.size());
      /*params[0] = 1000;
      params[1] = 3600;
      params[2] = 80;
      params[3] = 0;
      params[4] = 19530;
      params[5] = 1720;*/
      printf("Parameters (estimated in %d steps) :\n", iterations);
      for (int i = 0; i < parameters.size(); i++) {
         printf("param[%i] = %lf\n", i, params[i]);
         *(parameters[i]) = params[i];
      }
      printf("Est. stats: %s\n", cost_m->MultiplyRight(params).ToString().c_str());
     
      for (void_f_void_set::iterator i = param_appliers.begin();
               i != param_appliers.end(); i++)
      {
         (*(*i))();
      }
      fflush(stdout);

      calculated = true;
   }
} goals;

template <typename RetVal>
struct TestAdder {
   TestAdder(int id, RetVal (*_Run)(),
                     void (*paramreg)(void),
                     void (*paramapply)(void))
   {
      CHECK(the_map_of_tests.count(id) == 0);
      the_map_of_tests[id] = Test(id, _Run);
      goals.AddPattern(paramreg, paramapply);
   }
};

#define REGISTER_PATTERN(id) TestAdder<void> add_test##id(id, Pattern##id, \
                             ParametersRegistration##id, ApplyParameters##id)
#define REGISTER_PATTERN_PROB(id) TestAdder<bool> add_test##id(id, Pattern##id, \
                             ParametersRegistration##id, ApplyParameters##id)

ThreadPool * mainThreadPool;
std::map<int, double> map_of_counts; // test -> average run count

inline double round(double lf) {
   return floor(lf + 0.5);
}

// Accessing memory locations holding one lock {{{1
namespace one_lock {
   struct Params {
      double num_contexts;     
      int NUM_CONTEXTS;

      double num_iterations_times_runcount;
      int NUM_ITERATIONS;
     
      //double data_size_times_;
      static const int DATA_SIZE = 128;
      static const int REDO_CNT  = 2;
   } params;
  
   struct TestContext {
      Mutex64 MU;
      int * data;
      TestContext() {
         data = new int[params.DATA_SIZE];
      }
   } *contexts;
  
   // Write accesses
   void Pattern101() {   
      int id = rand() % params.NUM_CONTEXTS;
      TestContext * context = &contexts[id];
      for (int i = 0; i < params.NUM_ITERATIONS; i++) {
         context->MU.Lock();
            for (int j = 0; j < params.DATA_SIZE; j++) {
               for (int k = 0; k < params.REDO_CNT; k++)
                  context->data[j] = 77; // write
            }
         context->MU.Unlock();
      }
   }
   void ParametersRegistration101() {
      map_of_counts[101] = 100;
      //goals.RegisterParameter(&map_of_counts[101]);
     
      goals.RegisterParameter(&params.num_iterations_times_runcount);
      goals.SetParameterStat(N_MUTEX_LOCK_UNLOCK, &params.num_iterations_times_runcount, 3 /*don't ask why*/);
      goals.SetParameterStat(N_MEM_ACCESSES_K, &params.num_iterations_times_runcount,
                             params.DATA_SIZE * params.REDO_CNT * 2.0 / 1000.0);
     
      goals.RegisterParameter(&params.num_contexts);
      goals.SetParameterStat(N_MUTEXES, &params.num_contexts, 1);
   }
   void ApplyParameters101() {
      if (map_of_counts[101] < 1.0)
         map_of_counts[101] = 1.0;
      params.NUM_CONTEXTS = round(params.num_contexts);
      if (params.NUM_CONTEXTS <= 0)
         params.NUM_CONTEXTS = 1;
      params.NUM_ITERATIONS = round(params.num_iterations_times_runcount / map_of_counts[101]);
     
      contexts = new TestContext[params.NUM_CONTEXTS];
   }
   REGISTER_PATTERN(101);
  
   /* other tests...
   // Read accesses
   void Pattern102() {
      int id = rand() % NUM_CONTEXTS;
      TestContext * context = &contexts[id];
      for (int i = 0; i < NUM_ITERATIONS; i++) {
         int temp = 0;
         context->MU.Lock();
            for (int j = 0; j < DATA_SIZE; j++) {
               for (int k = 0; k < 10; k++)
                  temp += context->data[j]; // read
            }
         context->MU.Unlock();
      }
   }
   REGISTER_PATTERN(102);
  

   int atomic_integers[NUM_CONTEXTS] = {0};
   // Atomic increment
   void Pattern103() {
      int id = rand() % NUM_CONTEXTS;
      for (int i = 0; i < NUM_ITERATIONS; i++)
         __sync_add_and_fetch(&atomic_integers[id], 1);
   }
   REGISTER_PATTERN(103);
   */
} // namespace one_lock
/* other namespaces...
// Accessing memory locations holding random LockSets {{{1
namespace multiple_locks {
   // TODO: make these constants as parameters
   const int NUM_CONTEXTS   = 1024;
   const int DATA_SIZE      = 4096;
   const int NUM_ITERATIONS = 1;
   const int LOCKSET_SIZE   = 2;
     
   struct TestContext {
      Mutex64 MU;
      int data[DATA_SIZE];
   } contexts[NUM_CONTEXTS];

   // Access random context holding a random LS including context->MU
   void Pattern201() {
      TestContext * context = &contexts[rand() % NUM_CONTEXTS];
      std::vector<Mutex64*> LS;
      // STL nightmare starts here - calculate random LS{{{1
      {
         std::vector<int> tmp_LS;
         for (int i = 0; i < NUM_CONTEXTS; i++)
            tmp_LS.push_back(i);
         std::random_shuffle(tmp_LS.begin(), tmp_LS.end());
        
         // TODO: #LS as a parameter
         for (int i = 0; i < LOCKSET_SIZE; i++)
            LS.push_back(&contexts[tmp_LS[i]].MU);
        
         // This LS should contain context's Mutex to have proper synchronization
         LS.push_back(&context->MU);        
        
         // LS should be sorted to avoid deadlocks
         std::sort(LS.begin(), LS.end());
        
         // LS should not contain context->MU twice
         std::vector<Mutex64*>::iterator new_end = std::unique(LS.begin(), LS.end());
         LS.erase(new_end, LS.end());
      } // end of STL nightmare :-)
     
      for (int i = 0; i < NUM_ITERATIONS; i++) {
         for (std::vector<Mutex64*>::iterator it = LS.begin(); it != LS.end(); it++)
            (*it)->Lock();
         for (int j = 0; j < DATA_SIZE; j++)
            context->data[j] = 77;
         for (std::vector<Mutex64*>::reverse_iterator it = LS.rbegin(); it != LS.rend(); it++)
            (*it)->Unlock();
      }
   }
   REGISTER_PATTERN(201);
  
   const int MAX_LOCKSET_SIZE   = 3;
   const int NUM_LOCKSETS = 1 << MAX_LOCKSET_SIZE;
   Mutex64 ls_mu[MAX_LOCKSET_SIZE];
   char ls_data[NUM_LOCKSETS][DATA_SIZE];
   // Access random context holding a corresponding LockSet
   void Pattern202() {
      int ls_idx = 0;     
      while (ls_idx == 0)
         ls_idx = rand() % NUM_LOCKSETS;
     
      char * data = ls_data[ls_idx];
      for (int i = 0; i < MAX_LOCKSET_SIZE; i++)
         if (ls_idx & (1 << i))
            ls_mu[i].Lock();
     
      for (int j = 0; j < DATA_SIZE; j++)
         data[j] = 77;
     
      for (int i = MAX_LOCKSET_SIZE - 1; i >= 0; i--)
         if (ls_idx & (1 << i))
            ls_mu[i].Unlock();
   }
   REGISTER_PATTERN(202);
} // namespace multiple_locks
*/

// Publishing objects using different synchronization patterns {{{1
namespace publishing {
   /*namespace pcq {
      const int NUM_CONTEXTS = 16;
     
      struct Params {
        double num_contexts;
        int NUM_CONTEXTS;

        double data_size_times_runcount;
        int DATA_SIZE;
      } params;

      struct TestContext {
         ProducerConsumerQueue pcq;
        
         TestContext() : pcq(0) {}
         ~TestContext() {
            void * ptr = NULL;
            // Erase the contents of the PCQ. We assume NULL can't be there
            pcq.Put(NULL);
            while(ptr = pcq.Get())
               free(ptr);
         }
      } * contexts;
  
      // Publish a random string into a random PCQ
      void Pattern301() {
         TestContext * context = &contexts[rand() % params.NUM_CONTEXTS];
         // TODO: str_len as a parameter
         int str_len = 1 + (rand() % params.DATA_SIZE);
         char * str = (char*)malloc(str_len + 1);
         CHECK(str != NULL);
         memset(str, 'a', str_len);
         str[str_len] = '\0';
         context->pcq.Put(str);
      }
      REGISTER_PATTERN(301);
     
      // Read a published string from a random PCQ. MAYFAIL!
      bool Pattern302() {
         TestContext * context = &contexts[rand() % NUM_CONTEXTS];
         char * str = NULL;
         if (context->pcq.TryGet((void**)&str)) {
            int tmp = strlen(str);
            free(str);
            return true;
         }
         return false;
      }
      REGISTER_PATTERN(302);
   }*/
  
   namespace condvar {
      struct Params {
         double num_contexts;
         int NUM_CONTEXTS;
        
         double data_size_times_runcount;
         int DATA_SIZE;
         Params() {
            DATA_SIZE = 1;
            HIT_PROBABILITY = 0.3; // estimate. TODO: think of a better idea
         }

         const static int REDO = 100;
         double HIT_PROBABILITY;

         double EstimateRuncount() {
            return map_of_counts[311] + HIT_PROBABILITY * map_of_counts[312];
         }
      } params;

      struct TestContext {
         Mutex64 MU;
         CondVar CV;
         int CV_Signalled;
        
         char * data;
         TestContext () {
            data = NULL;
            CV_Signalled = 0;
         }
      } *contexts;
  
      // Signal a random CV
      void Pattern311() {
         int id = rand() % params.NUM_CONTEXTS;
         TestContext * context = &contexts[id];
         context->MU.Lock();
         if (context->data) {
            free(context->data);
         }
         int LEN = params.DATA_SIZE;
         context->data = (char*)malloc(LEN + 1);
         for (int i = 0; i < params.REDO; i++)
            for (int j = 0; j < LEN; j++)
               context->data[j] = 'a';
         context->data[LEN] = '\0';
         context->CV.Signal();
         context->CV_Signalled = params.NUM_CONTEXTS;
         context->MU.Unlock();
      }
      void ParametersRegistration311() {
         double * num_CV_Signals = &map_of_counts[311];
         goals.RegisterParameter(num_CV_Signals);
         goals.SetParameterStat(N_CV_SIGNALS, num_CV_Signals, 1);
         goals.SetParameterStat(N_MUTEX_LOCK_UNLOCK, num_CV_Signals, 9 /* don't ask why */);
        
         goals.RegisterParameter(&params.num_contexts);
         goals.SetParameterStat(N_CV, &params.num_contexts, 1);
         goals.SetParameterStat(N_MUTEXES, &params.num_contexts, 1);

         goals.RegisterParameter(&params.data_size_times_runcount);
         goals.SetParameterStat(N_MEM_ACCESSES_K, &params.data_size_times_runcount,
                                  (2*params.REDO) * (1.0 + params.HIT_PROBABILITY) / 1000.0);
      }
      void ApplyParameters311() {
        if (map_of_counts[311] < 1.0)
          map_of_counts[311] = 1.0;

        params.DATA_SIZE = 1 + round(params.data_size_times_runcount / params.EstimateRuncount());
        /*if (params.DATA_SIZE < 1)
          params.DATA_SIZE = 1;*/

        params.NUM_CONTEXTS = round(params.num_contexts);
        if (params.NUM_CONTEXTS < 1)
          params.NUM_CONTEXTS = 1;
        contexts = new TestContext[params.NUM_CONTEXTS];
      }
      /*void TMP311 ()  {
        map_of_counts[311]  = 1000;
        params.NUM_CONTEXTS = 100;
        params.DATA_SIZE    = 10000;
        contexts = new TestContext[params.NUM_CONTEXTS];
      }*/
      REGISTER_PATTERN(311);
     
      // Wait on a random CV
      bool Pattern312() {
         int nAttempts = 0,
             id = rand() % params.NUM_CONTEXTS;
         TestContext * context;
                          
         do {
            if (nAttempts++ > params.NUM_CONTEXTS)
               return false;
            context = &contexts[id];
            id = (id + 1) % params.NUM_CONTEXTS;
         } while (ANNOTATE_UNPROTECTED_READ(context->CV_Signalled) == 0);
        
         context->MU.Lock();
         context->CV_Signalled--;
         bool ret = !context->CV.WaitWithTimeout(&context->MU, 10);
         if (ret && context->data) {
            // int tmp = strlen(context->data);
            free(context->data);
            context->data = NULL;
         }
         context->MU.Unlock();
         return ret;
      }
      void ParametersRegistration312() {
         double * num_CV_Waits = &map_of_counts[312];
         goals.RegisterParameter(num_CV_Waits);
         goals.SetParameterStat(N_CV_WAITS, num_CV_Waits, params.HIT_PROBABILITY);
         goals.SetParameterStat(N_MUTEX_LOCK_UNLOCK, num_CV_Waits, 16);
         // N_MEM_ACCESSES_K is counted in ParametersRegistration312
      }
      void ApplyParameters312() {
         //nothing to do, see ApplyParameters311
      }
      REGISTER_PATTERN_PROB(312);
   }
} // namespace publishing

/*
// Threads work with their memory exclusively {{{1
namespace thread_local {
   // Thread accesses heap
   void Pattern401() {
      // TODO: parameters
      const int DATA_SIZE  = 1024;
      const int ITERATIONS = 16;
     
      char * temp = (char*)malloc(DATA_SIZE + 1);
      for (int i = 1; i <= ITERATIONS; i++) {
         memset(temp, i, DATA_SIZE);
         temp[DATA_SIZE] = 0;
         int size = strlen(temp);
      }
      free(temp);
   }
   REGISTER_PATTERN(401);
  
   // Thread accesses stack
   void Pattern402() {
      // TODO: parameters
      const int DATA_SIZE  = 1024;
      const int ITERATIONS = 16;
     
      char temp[DATA_SIZE];
      for (int i = 1; i <= ITERATIONS; i++) {
         memset(temp, i, DATA_SIZE);
         temp[DATA_SIZE] = 0;
         int size = strlen(temp);
      }
   }
   REGISTER_PATTERN(402);
} // namespace thread_local

// Different benign races scenarios {{{1
namespace benign_races {
   namespace stats {
      int simple_counter = 0;
     
      int odd_counter = 1;
      Mutex64 odd_counter_mu;
     
      struct __ {
         __() {
            ANNOTATE_BENIGN_RACE(&simple_counter, "Pattern501");
         }
      } _;
  
      void Pattern501() {
         simple_counter++;
      }
      REGISTER_PATTERN(501);
     
      // increment odd_counter, but first check it is >0 (double-check)
      void Pattern502() {
         if (ANNOTATE_UNPROTECTED_READ(odd_counter) > 0) {
            odd_counter_mu.Lock();
            if (odd_counter > 0)
               odd_counter++;
            odd_counter_mu.Unlock();
         }
      }
      REGISTER_PATTERN(502);
   }
     
} // namespace benign_races
*/

typedef std::map<std::string, StatType> StatMap;
StatMap statNames;
int nThreads = 2;

void PatternDispatcher() {  
   /*std::vector<int> availablePatterns;
   for (MapOfTests::iterator it = the_map_of_tests.begin();
         it != the_map_of_tests.end(); it++) {
      if (map_of_counts[it->first] > 0.0)
         availablePatterns.push_back(it->first);
   }*/

   std::map<int, int> this_thread_runcounts;
  
   int total = 0;
   for (std::map<int,double>::iterator it = map_of_counts.begin(); it != map_of_counts.end(); it++) {
      CHECK(it->second >= 0.0);
      int count = round(it->second / nThreads);
      this_thread_runcounts[it->first] = count;
      total += count;
   }
  
   CHECK(total > 0);
    
   for (int i = 0; i < total; i++) {
   //while (total > 0) {
      int rnd = rand() % total;
      int test_idx = -1;
      for (std::map<int,int>::iterator it = this_thread_runcounts.begin(); it != this_thread_runcounts.end(); it++) {
         int this_test_count = it->second;
         if (rnd < this_test_count) {
            test_idx = it->first;
            break;
         }
         rnd -= this_test_count;
      }
      CHECK(test_idx >= 0);
      // TODO: the above code should be replaced with a proper randomizer
      // with a "specify distribution function" feature
      if (the_map_of_tests[test_idx].Run()) {
      /*   this_thread_runcounts[test_idx]--;
         total--;*/
      }
   }
}

namespace N_THREADS_hack {
  double nThreads_double = 2.0;

  void Registerer() {
    goals.RegisterParameter(&nThreads_double);
    goals.SetParameterStat(N_THREADS, &nThreads_double, 1);
  }

  void Applier() {
    nThreads = round(nThreads_double);
    CHECK(nThreads >= 2);
  }
}

void RegisterStatNames() {
#define REGISTER_STAT_NAME(a) statNames[#a] = a
  goals.AddPattern(N_THREADS_hack::Registerer, N_THREADS_hack::Applier);
  REGISTER_STAT_NAME(N_THREADS);
  REGISTER_STAT_NAME(N_CV);
  REGISTER_STAT_NAME(N_CV_SIGNALS);
  REGISTER_STAT_NAME(N_CV_WAITS);
  REGISTER_STAT_NAME(N_MUTEXES);
  REGISTER_STAT_NAME(N_MUTEX_LOCK_UNLOCK);
  REGISTER_STAT_NAME(N_MEM_ACCESSES_K);
}

int main(int argc, const char **argv) {
   long init = GetTimeInMs();
   RegisterStatNames();
   const char *default_goals[] = {"N_THREADS=20", "N_MEM_ACCESSES_K=130000",
        "N_MUTEXES=1800", "N_CV=80", "N_MUTEX_LOCK_UNLOCK=107000",
        "N_CV_SIGNALS=3600", "N_CV_WAITS=500"};
   const char ** goal_list = NULL;
   int goal_cnt = 0;
   if (argc == 1) {
      printf("Running the default pattern\n");
      goal_list = default_goals;
      goal_cnt  = sizeof(default_goals) / sizeof(*default_goals);
   } else if (argc == 2 && !strcmp(argv[1], "--help")) {
      printf("Usage: bigtest [PARAM=VALUE] ...\n  Available params: ");
      for (StatMap::iterator i = statNames.begin(); i != statNames.end(); i++) {
         printf ("%s%s", (i == statNames.begin()) ? "" : ", ",
                         (*i).first.c_str());
      }
      printf("\n");
      return 0;
   } else {
     goal_list = argv + 1;
     goal_cnt  = argc - 1;
   }
  
   {
      // Parse goal strings
      for (int i = 0; i < goal_cnt; i++) {
         const char * goal = goal_list[i];
         char stat[256] = "";
         int stat_val = -1, j = 0;
         for (; j < sizeof(stat) - 1
                  && goal[j] != '='
                  && goal[j] != '\0'; j++) {
            stat[j] = goal[j];
         }
         stat[j] = '\0';
         if (goal[j] == '=')
             sscanf(goal + j + 1, "%i", &stat_val);
         printf("%s = %i\n", stat, stat_val);
         if (goal[j] != '='
             || strlen(stat) == 0
             || stat_val < 0
             || statNames.find(stat) == statNames.end()
             ) {
            fprintf(stderr, "Error parsing goal \"%s\"\n", goal);
            CHECK(0);
         }
         goals.AddGoal(statNames[stat], stat_val);
      }
      printf("\n");
   }
   goals.CompileStatsIntoVector();
   Vector statsVector = goals.GetStatsVector();
   goals.RegisterPatterns();
   goals.CalculateAndApplyParameters();/**/
   long start = GetTimeInMs();
   printf("\nParameters calculated in %dms\nBenchmarking...\n",
          (int)(start - init));
   // Start (N_THREADS - 1) new threads...
   mainThreadPool = new ThreadPool(nThreads - 1);
   mainThreadPool->StartWorkers();
   for (int i = 0; i < nThreads - 1; i++) {
      mainThreadPool->Add(NewCallback(PatternDispatcher));
   }
   PatternDispatcher(); // and 1 more in the main thread
   delete mainThreadPool;
   long end = GetTimeInMs();
   printf("...done in %dms\n", (int)(end - start));
   printf("*RESULT bigtest: time= %d ms\n", (int)(end - start));
   return 0;
}
