import numpy as np
import csv

iterations = []
army1 = []
army2 = []
actual_counts = []

with open("../worker/big_metrics.csv") as file:
    csv_reader = csv.reader(file, delimiter=',')
    for row in csv_reader:
        iterations.append(int(row[1]))
        army1.append(int(row[2]))
        army2.append(int(row[3]))
        actual_counts.append(int(row[5]))

iterations = np.array(iterations)
army1 = np.array(army1)
army2 = np.array(army2)
actual_counts = np.array(actual_counts)

print(len(iterations))

#print("Iterations:", iterations)
#print("Army 1:", army1)
#print("Army 2:", army2)
#print("Actual counts:", actual_counts)



def sort_csv():
    with open("../worker/big_metrics.csv") as file:
        csv_reader = csv.reader(file, delimiter=',')
        data = list(csv_reader)
        # sort according to row 2 3 and 4
        data.sort(key=lambda x: (int(x[2]),int(x[3]), int(x[4])))
        with open("../worker/big_metrics_sorted.csv", "w") as f:
            csv_writer = csv.writer(f, delimiter=',')
            csv_writer.writerows(data)

def reg(ite, a1, a2, actual_count, weights):
    prev =  (abs(a1-a2)*weights[0] + (a1+a2)/2*weights[1] + ite*weights[2])*10000000
    return np.abs(prev-actual_count)

def run_reg(weights):
    diffs = []
    for i in range(2475):
        diff = reg(iterations[i], army1[i], army2[i], actual_counts[i], weights)
        diffs.append(diff)
        
    print("Weights:", weights)
    print("Avg diff:", np.mean(diffs))
    #print("Max diff:", np.max(diffs))
    #print("Median diff:", np.median(diffs))
    return np.mean(diffs)


from itertools import permutations

def find_combinations(target_sum, current_sum, start, combination):
    result = []
    if current_sum == target_sum and len(combination) == 3:
        perms = permutations(combination)
        unique_perms = set(perms)
        result.extend(unique_perms)
        return result
    elif current_sum > target_sum or len(combination) >= 3:
        return result
    for i in range(start, target_sum - current_sum + 1):
        combination.append(i)
        result.extend(find_combinations(target_sum, current_sum + i, i, combination))
        combination.pop()
    return result

combinations = find_combinations(100, 0, 1, [])

all_medians = []
for c in combinations:
    # divide by 100 to get the actual weights
    c = [x/100 for x in c]
    all_medians.append(run_reg(c))
    print("")

print("Min avg abs diff: ", np.min(all_medians))



