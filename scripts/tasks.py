import argparse
import requests
import matplotlib.pyplot as plt
import csv
import numpy as np
import base64
from scipy.optimize import curve_fit


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
        [1, 100, 1],  # max,
        [1, 50, 10],  # army1,
        [1, 50, 10]  # army2
    ]

    # range [min, max]
    simulate_args = [
        [1, 1000, 50],  # generations,
        [1, 5, 1],  # world, NOTA VAI ATE 4 MAS MAIS 1 NO RANGE
        [1, 4, 1]  # scenario NOTA VAI ATE 4 MAS MAIS 1 NO RANGE
    ]

    # for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
    #     war_endpoint = "http://localhost:8000/insectwar?max={}&army1=100&army2=100".format(
    #         i)
    #     print(war_endpoint)
    #     requests.get(war_endpoint)

    # for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
    #     war_endpoint = "http://localhost:8000/insectwar?max={}&army1=50&army2=50".format(
    #         i)
    #     print(war_endpoint)
    #     requests.get(war_endpoint)

    # for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
    #     war_endpoint = "http://localhost:8000/insectwar?max={}&army1=200&army2=200".format(
    #         i)
    #     print(war_endpoint)
    #     requests.get(war_endpoint)

    # for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
    #     for j in range(war_args[1][0], war_args[1][1], war_args[1][2]):  # army1
    #         for k in range(war_args[2][0], war_args[2][1], war_args[2][2]):  # army2
    #             war_endpoint = "http://localhost:8000/insectwar?max={}&army1={}&army2={}".format(
    #                 i, j, k)
    #             print(war_endpoint)
    #             requests.get(war_endpoint)

    # for j in range(war_args[1][0], war_args[1][1], war_args[1][2]):  # army1
    #     for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
    #         for k in range(war_args[2][0], war_args[2][1], war_args[2][2]):  # army2
    #             war_endpoint = "http://localhost:8000/insectwar?max={}&army1={}&army2={}".format(
    #                 i, j, k)
    #             requests.get(war_endpoint)
    #         break
    #     break

    # for i in range(war_args[0][0], war_args[0][1], war_args[0][2]):  # max
    #     for k in range(war_args[2][0], war_args[2][1], war_args[2][2]):  # army2
    #         for j in range(war_args[1][0], war_args[1][1], war_args[1][2]):  # army1
    #             war_endpoint = "http://localhost:8000/insectwar?max={}&army1={}&army2={}".format(
    #                 i, j, k)
    #             requests.get(war_endpoint)
    #         break
    #     break

    # for j in range(war_args[1][0], war_args[1][1], war_args[1][2]):  # army1 and army2
    #     war_endpoint = "http://localhost:8000/insectwar?max=100&army1={}&army2={}".format(
    #         j, j)
    #     requests.get(war_endpoint)

    # for k in range(simulate_args[2][0], simulate_args[2][1], simulate_args[2][2]):  # scenario
    #     for j in range(simulate_args[1][0], simulate_args[1][1], simulate_args[1][2]):  # world
    #         # generations
    #         for i in range(simulate_args[0][0], simulate_args[0][1], simulate_args[0][2]):
    #             simulate_endpoint = "http://localhost:8000/simulate?generations={}&world={}&scenario={}".format(
    #                 i, j, k)
    #             requests.get(simulate_endpoint)
    #         break
    #     break

    # for k in range(simulate_args[2][0], simulate_args[2][1], simulate_args[2][2]):  # scenario
    #     simulate_endpoint = "http://localhost:8000/simulate?generations=10&world=1&scenario={}".format(
    #         k)
    #     requests.get(simulate_endpoint)

    # for i in range(simulate_args[0][0], simulate_args[0][1], simulate_args[0][2]): # generations
    #     for k in range(simulate_args[2][0], simulate_args[2][1], simulate_args[2][2]):  # scenario
    #         for j in range(simulate_args[1][0], simulate_args[1][1], simulate_args[1][2]): # world
    #             simulate_endpoint = "http://localhost:8000/simulate?generations={}&world={}&scenario={}".format(i, j, k)
    #             print(simulate_endpoint)
    #             requests.get(simulate_endpoint)


def sendImages():

    compression_args = [
        [
            "1024x768.bmp", "1920x1280.bmp", 
            "1024x768_2.bmp", "640x426.bmp", 
            "1280x853.bmp", "640x480.bmp",

            "651x502.png","1024x1024.png",
            "1200x1098.png","1600x1124.png",
            "3500x1142.png",

            "800x400.jpeg","1024x768.jpeg",
            "1320x880.jpeg","1642x1094.jpeg",
            "2048x1536.jpeg",
        ],
        [0, 10, 2]
    ]

    for k in range(compression_args[1][0], compression_args[1][1], compression_args[1][2]):  # compressionFactor
        for img in compression_args[0]:  # images
            dir = "images/"+img
            with open(dir, "rb") as image:
                payload = "targetFormat:{};compressionFactor:{};data:image/{};base64,{}".format(
                    img.split(".")[1], 
                    k*0.1, img.split(".")[1], 
                    base64.b64encode(image.read()).decode("utf-8")
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
            equation = "y={:.8e}x+{:.8e}".format(slope, intercept)
            print("{:.8e}".format(slope))
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

            # the line below are about exponential stuff
            # arr = np.polyfit(x, np.log(instructions),1, w=np.sqrt(instructions))
            # slope, intercept = arr
            # equation = "y={:.3e}+exp({:.3e}x)".format(intercept,slope)
            # print(arr)

            # def exponential_2(x, a, b, c):
            #    return a * (x**(b*2)) + c

            # popt, pcov = curve_fit(exponential_2, x, instructions)
            # print(popt)
            # print(pcov)
            # equation = "y={:.5e}*(x**({:.5e}*2))+{:.5e}".format(popt[0],popt[1], popt[2])

            # plot.set_title(equation)
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
                            if endpoint == "compress":
                                h,w = row[1].split("x")
                                x.append(int(h)*int(w))
                            else:
                                x.append(int(row[x_idx]))
                            basicBlocks.append(int(row[4]))
                            instructions.append(int(row[5]))

            plot.set_title(compute_slope_equation(x, instructions))
            plot.set_xlabel(x_label)
            plot.scatter(x, basicBlocks, label='Basic Blocks')
            plot.scatter(x, instructions, label='Instructions')

        SIZE = (3, 4)

        # # Multiple generations for all scenarios of world 1
        # draw_plot(0, 0, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 1 and int(row[3]) == 1)
        # draw_plot(1, 0, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 1 and int(row[3]) == 2)
        # draw_plot(2, 0, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 1 and int(row[3]) == 3)

        # # Multiple generations for all scenarios of world 2
        # draw_plot(0, 1, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 2 and int(row[3]) == 1)
        # draw_plot(1, 1, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 2 and int(row[3]) == 2)
        # draw_plot(2, 1, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 2 and int(row[3]) == 3)

        # # Multiple generations for all scenarios of world 3
        # draw_plot(0, 2, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 3 and int(row[3]) == 1)
        # draw_plot(1, 2, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 3 and int(row[3]) == 2)
        # draw_plot(2, 2, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 3 and int(row[3]) == 3)

        # # Multiple generations for all scenarios of world 4
        # draw_plot(0, 3, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 3 and int(row[3]) == 1)
        # draw_plot(1, 3, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 3 and int(row[3]) == 2)
        # draw_plot(2, 3, "simulate", 1, "Generations",
        #           lambda row: int(row[2]) == 3 and int(row[3]) == 3)

        # Multiple images, multiple compressions, PNG

        # Multiple images, multiple compressions, BMP
        # draw_scatter(0,0,"compress", 1, "Pixeis", lambda row: row[2] == "bmp" and float(row[3]) == 0.0)
        # draw_scatter(0,1,"compress", 1, "Pixeis", lambda row: row[2] == "bmp" and float(row[3]) == 0.2)
        # draw_scatter(0,2,"compress", 1, "Pixeis", lambda row: row[2] == "bmp" and float(row[3]) == 0.4)
        # draw_scatter(1,0,"compress", 1, "Pixeis", lambda row: row[2] == "bmp" and float(row[3]) == 0.6)
        # draw_scatter(1,1,"compress", 1, "Pixeis", lambda row: row[2] == "bmp" and float(row[3]) == 0.8)

        # Multiple images, multiple compressions, PNG
        # draw_scatter(0,0,"compress", 1, "Pixeis", lambda row: row[2] == "png" and float(row[3]) == 0.0)
        # draw_scatter(0,1,"compress", 1, "Pixeis", lambda row: row[2] == "png" and float(row[3]) == 0.2)
        # draw_scatter(0,2,"compress", 1, "Pixeis", lambda row: row[2] == "png" and float(row[3]) == 0.4)
        # draw_scatter(1,0,"compress", 1, "Pixeis", lambda row: row[2] == "png" and float(row[3]) == 0.6)
        # draw_scatter(1,1,"compress", 1, "Pixeis", lambda row: row[2] == "png" and float(row[3]) == 0.8)

        # Multiple images, multiple compressions, JPEG
        draw_scatter(0,0,"compress", 1, "Pixeis", lambda row: row[2] == "jpeg" and float(row[3]) == 0.0)
        draw_scatter(0,1,"compress", 1, "Pixeis", lambda row: row[2] == "jpeg" and float(row[3]) == 0.2)
        draw_scatter(0,2,"compress", 1, "Pixeis", lambda row: row[2] == "jpeg" and float(row[3]) == 0.4)
        draw_scatter(1,0,"compress", 1, "Pixeis", lambda row: row[2] == "jpeg" and float(row[3]) == 0.6)
        draw_scatter(1,1,"compress", 1, "Pixeis", lambda row: row[2] == "jpeg" and float(row[3]) == 0.8)
        
        
        # for requests in line 72
        # draw_scatter(0, 0, "war", 1, "Iterations", lambda row: int(row[2]) == 100 and int(row[3]) == 100)
        # draw_scatter(0, 1, "war", 1, "Iterations", lambda row: int(row[2]) == 50 and int(row[3]) == 50)
        # draw_scatter(0, 2, "war", 1, "Iterations", lambda row: int(row[2]) == 200 and int(row[3]) == 200)

        # draw_plot(0, 0, "war", 1, "Iterations", lambda row: int(row[2]) == 11 and int(row[3]) == 41)
        # draw_plot(1, 0, "war", 1, "Iterations", lambda row: int(row[2]) == 41 and int(row[3]) == 41)
        # draw_plot(2, 0, "war", 1, "Iterations", lambda row: int(row[2]) == 11 and int(row[3]) == 11)

        # draw_plot(0, 0, "war", 1, "Iterations", lambda row: int(row[2]) == 100 and int(row[3]) == 100)
        # draw_plot(0, 1, "war", 3, "Army1 Size", lambda row: int(row[1]) == 1 and int(row[2]) == 1)
        # draw_plot(0, 2, "war", 2, "Army2 Size", lambda row: int(row[1]) == 1 and int(row[3]) == 1)
        # draw_plot(0, 2, "war", 2, "Army1==Army2 Size", lambda row: int(row[1]) == 100 and int(row[3]) == int(row[2]))
        # draw_plot(1, 0, "simulate", 1, "Generations", lambda row: int(row[2]) == 1 and int(row[3]) == 1)
        # draw_plot(1, 1, "simulate", 3, "Scenario", lambda row: int(row[1]) == 10 and int(row[2]) == 1)
        # draw_scatter(1, 2, "simulate", 2, "World Size", lambda row: int(row[1]) == 1 and int(row[3]) == 1)

        plt.tight_layout()
        plt.legend(loc='lower center', bbox_to_anchor=(-1.5, -1.5))
        plt.show()
