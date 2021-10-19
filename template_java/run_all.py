import subprocess

subprocess.run('./build.sh')
# Finding server
with open('../config_files/configs/perfect_link.txt', 'r') as f:
    line = f.readlines()[0].split(' ')
    server_id = int(line[0])

processes = []
finished = 0

# Finding and running all hosts
with open('../config_files/hosts.txt', 'r') as f:
    for l in f.readlines():
        perfect_link_id = int(l.split(' ')[0])
        process = subprocess.Popen(["./run.sh", "--id", str(perfect_link_id), '--hosts', '../config_files/hosts.txt',
                                    '--output', '../config_files/outputs/', '../config_files/configs/perfect_link.txt'],
                                   stdout=subprocess.DEVNULL)
        processes.append((perfect_link_id, process))

# Waiting for all except server to finish
for (perfect_link_id, p) in processes:
    if perfect_link_id != server_id:
        p.wait()

# Killing server
with open('../config_files/pid.txt', 'r') as f:
    subprocess.run(['kill', '-SIGINT', str(f.readlines()[0])])

for (perfect_link_id, p) in processes:
    if perfect_link_id == server_id:
        p.wait()
