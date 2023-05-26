import argparse
import requests
import matplotlib.pyplot as plt
import csv
import numpy as np
import base64

def sendRequest(endpoint, *args, image_files=[]):

    if len(image_files) > 0:
        # TODO: send image files
        for file in image_files:
            # files = {'file': open(file, 'rb')}
            # requests.post(endpoint, files=files)
            print(endpoint + " " + file)
    else:
        if len(args) == 0:
            print(f"Succefull request :{endpoint}")
            res = requests.get(endpoint)
            if res.status_code == 200:
                print(f"Succefull request :{endpoint}")
            else:
                print(f"Error {res.status_code} on request:  {endpoint}")
        else:
            arg = args[0]
            step = arg[2]
            for i in range(arg[0], arg[1], step):
                format = endpoint.format(i, *(["{}"] * (len(args) - 1)))
                sendRequest(format, *args[1:])


def sendAllRequests():

    # range [min, max]
    war_args = [
        [1, 100, 10],  # max,
        [1, 50, 10],  # army1,
        [1, 50, 10]  # army2
    ]

    # range [min, max]
    simulate_args = [
        [1, 100, 10],  # generations,
        [1, 5, 1],  # world, NOTA VAI ATE 4 MAS MAIS 1 NO RANGE
        [1, 4, 1]  # scenario NOTA VAI ATE 4 MAS MAIS 1 NO RANGE
    ]

    for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
        war_endpoint = "http://localhost:8000/insectwar?max={}&army1=100&army2=100".format(
            i)
        requests.get(war_endpoint)

    for j in range(war_args[1][0], war_args[1][1], war_args[1][2]):  # army1
        for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
            for k in range(war_args[2][0], war_args[2][1], war_args[2][2]):  # army2
                war_endpoint = "http://localhost:8000/insectwar?max={}&army1={}&army2={}".format(
                    i, j, k)
                requests.get(war_endpoint)
            break
        break

    for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
        for k in range(war_args[2][0], war_args[2][1], war_args[2][2]):  # army2
            for j in range(war_args[1][0], war_args[1][1], war_args[1][2]):  # army1
                war_endpoint = "http://localhost:8000/insectwar?max={}&army1={}&army2={}".format(
                    i, j, k)
                requests.get(war_endpoint)
            break
        break

    for j in range(war_args[1][0], war_args[1][1], war_args[1][2]):  # army1 and army2
        war_endpoint = "http://localhost:8000/insectwar?max=100&army1={}&army2={}".format(
            j, j)
        requests.get(war_endpoint)

    for k in range(simulate_args[2][0], simulate_args[2][1], simulate_args[2][2]):  # scenario
        for j in range(simulate_args[1][0], simulate_args[1][1], simulate_args[1][2]):  # world
            # generations
            for i in range(simulate_args[0][0], simulate_args[0][1], simulate_args[0][2]):
                simulate_endpoint = "http://localhost:8000/simulate?generations={}&world={}&scenario={}".format(
                    i, j, k)
                requests.get(simulate_endpoint)
            break
        break

    for k in range(simulate_args[2][0], simulate_args[2][1], simulate_args[2][2]):  # scenario
        simulate_endpoint = "http://localhost:8000/simulate?generations=10&world=1&scenario={}".format(
            k)
        requests.get(simulate_endpoint)

    for k in range(simulate_args[2][0], simulate_args[2][1], simulate_args[2][2]):  # scenario
        # generations
        for i in range(simulate_args[0][0], simulate_args[0][1], simulate_args[0][2]):
            # world
            for j in range(simulate_args[1][0], simulate_args[1][1], simulate_args[1][2]):
                simulate_endpoint = "http://localhost:8000/simulate?generations={}&world={}&scenario={}".format(
                    i, j, k)
                requests.get(simulate_endpoint)
            break
        break


def sendImages():

    compression_args = [
        [
            "meme.png",
            "meme1.jpg",
            "meme2.jpg",
        ],
        [0, 10, 2]
    ]

    for k in range(compression_args[1][0], compression_args[1][1], compression_args[1][2]):  # compressionFactor
        with open("meme.png", "rb") as image:
            payload = "targetFormat:{};compressionFactor:{};data:image/{};base64,{}".format(
                "png", k*0.1, "png", base64.b64encode(image.read()).decode("utf-8")  
            )
            requests.post("http://localhost:8000/compressimage", data=payload)


    for img in compression_args[0]:  # images
        with open(img, "rb") as image:
            payload = "targetFormat:{};compressionFactor:{};data:image/{};base64,{}".format(
                img.split(".")[1], "0.5", img.split(".")[1], base64.b64encode(image.read()).decode("utf-8")  
            )
            requests.post("http://localhost:8000/compressimage", data=payload)


if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('-a', '--analyse', action='store_true')
    group.add_argument('-r', '--requests', action='store_true')
    group.add_argument('-i', '--images', action='store_true')
    args = parser.parse_args()

    if args.images:
        sendImages()

    if args.requests:
        sendAllRequests()
        print("Completed")

    if args.analyse:
        
        def compute_slope_equation(x, instructions):
            slope, intercept = np.polyfit(x, instructions, 1)
            equation = "y={:.3e}x+{:.3e}".format(slope, intercept)
            return equation

        def draw_plot(x_grid, y_grid, endpoint, x_idx, x_label, condition):
            x = []
            basicBlocks = []
            instructions = []

            plot = plt.subplot2grid(SIZE, (x_grid, y_grid))
            with open("../worker/metrics.csv", "r") as csvfile:
                rows = csv.reader(csvfile, delimiter=',')
                for row in rows:
                    if row[0] == endpoint:
                        if condition(row):
                            x.append(int(row[x_idx]))
                            basicBlocks.append(int(row[4]))
                            instructions.append(int(row[5]))

            plot.set_title(compute_slope_equation(x, instructions))
            plot.set_xlabel(x_label)
            plot.plot(x, basicBlocks, label='Basic Blocks')
            plot.plot(x, instructions, label='Instructions')

        def draw_scatter(x_grid, y_grid, endpoint, x_idx, x_label, condition):
            x = []
            basicBlocks = []
            instructions = []

            plot = plt.subplot2grid(SIZE, (x_grid, y_grid))
            with open("../worker/metrics.csv", "r") as csvfile:
                rows = csv.reader(csvfile, delimiter=',')
                for row in rows:
                    if row[0] == endpoint:
                        if condition(row):
                            x.append(int(row[x_idx]))
                            basicBlocks.append(int(row[4]))
                            instructions.append(int(row[5]))

            plot.set_title(compute_slope_equation(x, instructions))
            plot.set_xlabel(x_label)
            plot.scatter(x, basicBlocks, label='Basic Blocks')
            plot.scatter(x, instructions, label='Instructions')

        SIZE = (3, 3)

        draw_plot(0, 0, "war", 1, "Iterations", lambda row: int(
            row[2]) == 100 and int(row[3]) == 100)
        draw_plot(0, 1, "war", 3, "Army1 Size", lambda row: int(
            row[1]) == 1 and int(row[2]) == 1)
        # draw_plot(0, 2, "war", 2, "Army2 Size", lambda row: int(row[1]) == 1 and int(row[3]) == 1)
        draw_plot(0, 2, "war", 2, "Army1==Army2 Size", lambda row: int(
            row[1]) == 100 and int(row[3]) == int(row[2]))
        draw_plot(1, 0, "simulate", 1, "Generations",
                  lambda row: int(row[2]) == 1 and int(row[3]) == 1)
        draw_plot(1, 1, "simulate", 3, "Scenario",
                  lambda row: int(row[1]) == 10 and int(row[2]) == 1)
        draw_scatter(1, 2, "simulate", 2, "World Size",
                     lambda row: int(row[1]) == 1 and int(row[3]) == 1)

        plt.tight_layout()
        plt.legend(loc='lower center', bbox_to_anchor=(-1.5, -1.5))
        plt.show()
