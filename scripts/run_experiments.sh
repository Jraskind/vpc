#This script loops through all of the auto-generated launch scripts for the given experiment folder
# THIS SHOULD BE RUN AS SUDO
# sudo bash run_experiment <experiment directory>
experiment_dir=$1"launch/*"
for file in $experiment_dir; do
    bash "$file"
done
