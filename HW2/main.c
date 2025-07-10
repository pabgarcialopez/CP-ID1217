/* Matrix summation using OpenMP
usage with gcc (version 4.2 or higher required):
gcc -O -fopenmp -o matrixSum-openmp matrixSum-openmp.c
./matrixSum-openmp size numWorkers
*/

#include <omp.h>
#include <stdio.h>

#define MAXSIZE 10000   // Maximum matrix size
#define MAXWORKERS 8    // Maximum number of workers

int numWorkers, size;
int matrix[MAXSIZE][MAXSIZE];
double start_time, end_time;

void *Worker(void *);

// Read command line, initialize, and create threads
int main(int argc, char *argv[]) {
    int i, j, total = 0;

    // Read command line args if any
    size = (argc > 1) ? atoi(argv[1]) : MAXSIZE;
    numWorkers = (argc > 2) ? atoi(argv[2]) : MAXWORKERS;
    if (size > MAXSIZE) size = MAXSIZE;
    if (numWorkers > MAXWORKERS) numWorkers = MAXWORKERS;

    omp_set_num_threads(numWorkers);

    // Initialize the matrix
    for (i = 0; i < size; i++) {
        // printf("[ ");
        for (j = 0; j < size; j++) {
            matrix[i][j] = rand()%99;
            // printf(" %d", matrix[i][j]);
        }
        // printf(" ]\n");
    }

    start_time = omp_get_wtime();

#pragma omp parallel for reduction (+:total) private(j)
    for (i = 0; i < size; i++) {
        for (j = 0; j < size; j++) {
            total += matrix[i][j];
        }
    } // Implicit barrier
    end_time = omp_get_wtime();
    printf("The total is %d\n", total);
    printf("It took %g seconds\n", end_time - start_time);
}
