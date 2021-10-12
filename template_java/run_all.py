import subprocess
import psutil

# Finding server
with open('../config_files/configs/perfect_link.txt', 'r') as f:
    l = f.readlines()[0].split(' ')
    server_id = int(l[0])

processes = []
finished = 0

# Finding and running all hosts
with open('../config_files/hosts.txt', 'r') as f:
    for l in f.readlines():
        id = int(l.split(' ')[0])
        process = subprocess.Popen(['wsl', "./run.sh", "--id", str(id), '--hosts', '../config_files/hosts.txt',
                                    '--output', '../config_files/outputs/', '../config_files/configs/perfect_link.txt'])
                                   #stdout=subprocess.DEVNULL)
        processes.append((id, process))

# Waiting for all except server to finish
for (id, p) in processes:
    if id != server_id:
        p.wait()
    else:
        parent = psutil.Process(p.pid).parent()
        children = parent.children()
        # all child pids can be accessed using the pid attribute
        child_pids = [p.pid for p in children]

# Killing server
print(child_pids)
print(server_id)
subprocess.run(['wsl', 'kill', '-SIGINT', str(child_pids[0])])
subprocess.run(['wsl', 'kill', '-SIGINT', str(child_pids[1])])
subprocess.run(['wsl', 'kill', '-SIGINT', str(child_pids[2])])

