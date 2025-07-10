/* matrix summation using pthreads

   features: uses a barrier; the Worker[0] computes
             the total sum from partial sums computed by Workers
             and prints the total sum to the standard output

   usage under Linux:
     gcc matrixSum.c -lpthread
     a.out size numWorkers

*/

#ifndef _REENTRANT
#define _REENTRANT
#endif

#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <sys/time.h>

#define MAXSIZE 10000      /* maximum matrix size */
#define MAXWORKERS 2        /* maximum number of workers */
#define MATRIX_LIMIT 200  /* maximum possible value of the matrix elements*/

pthread_mutex_t rowLock; /* mutex lock for the row (task) */
pthread_mutex_t dataLock; /* mutex lock for the data (sum, max and min) */

int numWorkers;           /* number of workers */
int nextRow = 0;          /* Bag of tasks */

struct Extreme {
    int value;
    int pos_i, pos_j;
};

int totalSum = 0;
struct Extreme minimum = {.value = MATRIX_LIMIT, .pos_i = -1, .pos_j = -1};
struct Extreme maximum = {.value = -1, .pos_i = -1, .pos_j = -1};

/* timer */
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

double start_time, end_time; /* start and end times */
int size;  /* assume size is multiple of numWorkers */
int matrix[MAXSIZE][MAXSIZE]; /* matrix */

void *Worker(void *);

struct Extreme min(struct Extreme a, struct Extreme b) {
    return a.value < b.value ? a : b;
}

struct Extreme max(struct Extreme a, struct Extreme b) {
    return a.value > b.value ? a : b;
}

int sumElementsInStrip(int first, int last) {
    int total = 0;
    for (int i = first; i <= last; i++)
        for (int j = 0; j < size; j++)
            total += matrix[i][j];

    return total;
}

// Returns the value and position of the minimum (mode = 0) or maximum (mode = 1) of matrix
struct Extreme obtainExtremeInStrip(int first, int last, int mode) {

    struct Extreme ext = {.value = MATRIX_LIMIT, .pos_i = -1, .pos_j = -1};

    if(mode == 1) // For max initialization (the matrix elements are at least 0).
        ext.value = -ext.value;

    for (int i = first; i <= last; i++) {
        for (int j = 0; j < size; j++) {
            if((mode == 0 && matrix[i][j] < ext.value) ||
               (mode == 1 && matrix[i][j] > ext.value)) {
                ext.value = matrix[i][j];
                ext.pos_i = i; ext.pos_j = j;
            }
        }
    }

    return ext;
}

/* read command line, initialize, and create threads */
int main(int argc, char *argv[]) {

    srand(time(0));

    int i, j;
    long l; /* use long in case of a 64-bit system */
    pthread_attr_t attr;
    pthread_t workerid[MAXWORKERS];

    /* set global thread attributes */
    pthread_attr_init(&attr);
    // Thread competes for resources with all the other threads of the system.
    pthread_attr_setscope(&attr, PTHREAD_SCOPE_SYSTEM);

    /* initialize mutexes and condition variable */
    pthread_mutex_init(&rowLock, NULL);
    pthread_mutex_init(&dataLock, NULL);


    /* read command line args if any */
    size = (argc > 1)? atoi(argv[1]) : MAXSIZE;
    numWorkers = (argc > 2)? atoi(argv[2]) : MAXWORKERS;
    if (size > MAXSIZE) size = MAXSIZE;
    if (numWorkers > MAXWORKERS) numWorkers = MAXWORKERS;

    /* initialize the matrix */
    for (i = 0; i < size; i++)
        for (j = 0; j < size; j++)
            matrix[i][j] = 1;//rand() % MATRIX_LIMIT;

    /* print the matrix */
#ifdef DEBUG
    for (i = 0; i < size; i++) {
        printf("[ ");
        for (j = 0; j < size; j++) {
            printf(" %d", matrix[i][j]);
        }
        printf(" ]\n");
    }
#endif

    /* Do the parallel work: create the workers */
    start_time = read_timer();

    for (l = 0; l < numWorkers; l++) {
        pthread_create(&workerid[l], &attr, Worker, NULL);
    }

    for (l = 0; l < numWorkers; l++) {
        pthread_join(workerid[l], NULL);
    }

    /* get end time */
    end_time = read_timer();
    /* print results */
    printf("The total sum is %d\n", totalSum);
    printf("The overall minimum is %d at position (%d, %d)\n", minimum.value, minimum.pos_i, minimum.pos_j);
    printf("The overall maximum is %d at position (%d, %d)\n", maximum.value, maximum.pos_i, maximum.pos_j);
    printf("The execution time is %g sec\n", end_time - start_time);

    pthread_exit(NULL);
}

/* Each worker sums the values in one strip of the matrix.
   After a barrier, worker(0) computes and prints results */
void * Worker(void *arg) {

    int row;

    while(true) {
        // Get a task
        pthread_mutex_lock(&rowLock);
        row = nextRow;
        nextRow++;
        pthread_mutex_unlock(&rowLock);

        if(row >= size)
            break;

        pthread_mutex_lock(&dataLock);
            // Sum values in my row, and obtain min and max of row.
            totalSum += sumElementsInStrip(row, row);;
            minimum = min(minimum, obtainExtremeInStrip(row, row, 0));
            maximum = max(maximum, obtainExtremeInStrip(row, row, 1));
        pthread_mutex_unlock(&dataLock);

    }

    return NULL;
}
