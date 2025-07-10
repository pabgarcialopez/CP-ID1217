#ifndef _REENTRANT
#define _REENTRANT
#endif

#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <sys/time.h>
#include <math.h>

#define MAXWORKERS 3        // Maximum number of additional allowed workers besides the main thread.

const double EPSILON = 10e-20 ;
int numWorkers = 0;
double startTime, endTime;
int numCreatedThreads = 1;

// Mutex lock for numCreatedThreads.
pthread_mutex_t createdThreadsLock;

struct Info {
    double a, b;     // Extreme points.
    double fa, fb;   // Evaluation of extreme points.
    double area;     // Area to be approximated.
};

double read_timer() {
    static bool initialized = false;
    static struct timeval start;
    struct timeval end;
    if(!initialized) {
        gettimeofday(&start, NULL);
        initialized = true;
    }
    gettimeofday(&end, NULL);
    return (end.tv_sec - start.tv_sec) + 1.0e-6 * (end.tv_usec - start.tv_usec);
}

void readCommandLine(int argc, char* argv[]) {
    numWorkers = (argc > 1)? atoi(argv[1]) : MAXWORKERS;
    if (numWorkers > MAXWORKERS || numWorkers < 1)
        numWorkers = MAXWORKERS;
}

double f(double x) {
    return sqrt(1 - x*x);
}

void displayResults(double piApprox) {
    printf("\n===========================RESULTS===========================\n");
    printf("Approximation of pi: %.10lf\n", 4 * piApprox);
    printf("The execution time is %g seconds.\n", endTime - startTime);
    printf("=============================================================\n");
}

void * calculatePI(void *args) {

    struct Info *info = args;

    // Calculate new data.
    double m = (info->a + info->b) / 2;
    double fm = f(m);
    double larea = (info->fa + fm) * (m - info->a) / 2;
    double rarea = (fm + info->fb) * (info->b - m) / 2;

    // Final result that will be returned.
    double *res = malloc(sizeof(double));

    // Check for termination.
    if(fabs(larea + rarea - info->area) < EPSILON) {
        *res = larea + rarea;
        return res;
    }

    // Boolean to control number of threads working.
    bool tooManyThreads = false;
    void* resultLeft, *resultRight;
    struct Info newLeft = {info->a, m, f(info->a), f(m), larea};
    struct Info newRight = {m, info->b, f(m), f(info->b), rarea};

    // Check whether we surpass the number of allowed threads.
    pthread_mutex_lock(&createdThreadsLock);
        tooManyThreads = numCreatedThreads + 1 > MAXWORKERS;
    pthread_mutex_unlock(&createdThreadsLock);

    if(!tooManyThreads) {

        // Update numCreatedThreads atomically.
        pthread_mutex_lock(&createdThreadsLock);
            numCreatedThreads++;
        pthread_mutex_unlock(&createdThreadsLock);

        // Execute left side with new thread, and right side with current thread.
        pthread_t newThread;
        pthread_create(&newThread, NULL, calculatePI, &newLeft);
        resultRight = calculatePI(&newRight);
        pthread_join(newThread, &resultLeft);

        // Update numCreatedThreads atomically.
        pthread_mutex_lock(&createdThreadsLock);
        numCreatedThreads--;
        pthread_mutex_unlock(&createdThreadsLock);
    }

    else {
        // Calculate the area of each side recursively on the same thread.
        resultLeft = calculatePI(&newLeft);
        resultRight = calculatePI(&newRight);
    }

    double * resultL = resultLeft;
    double * resultR = resultRight;
    *res = *resultL + *resultR;
    return res;
}

int main(int argc, char* argv[]) {

    // Read command line args if any.
    readCommandLine(argc, argv);

    // Initialize createdThreads mutex;
    pthread_mutex_init(&createdThreadsLock, NULL);

    // Initialize first information.
    struct Info info = {
            .a = 0,
            .b = 1,
            .fa = f(0),
            .fb = f(1),
            .area = (f(0) + f(1)) / 2
    };

    startTime = read_timer();

    // Obtain approximation of pi/4
    double * piApproximation = (double *) calculatePI(&info);

    endTime = read_timer();
    displayResults(*piApproximation);

    // 3.14159265358979323846264338327950288419716939937510

    return 0;
}
