#! /bin/bash

rm log.txt
#for FILE in $1
find $1 -type f | while read FILE1
do
	filename1=$(basename $FILE1)
	if [[ $filename1 =~ \.java$ ]] ;
	then
		find $2 -type f | while read FILE2
		do
			filename2=$(basename $FILE2)
			if [[ $filename2 =~ \.java$ ]] ;
			then
				if [ ${filename1} = ${filename2} ] ;
				then
					gumtree cluster $FILE1 $FILE2 | grep "move-tree" | wc -l >> ./log.txt
					break
				fi
			fi
		done
	fi 
done
cat log.txt | awk '{sum+=$1}END{print sum}'
