#! /bin/bash

rm ./log.txt
rm ./insert.txt
rm ./delete.txt
rm ./move.txt
rm ./update.txt

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
					diffresult=`diff $FILE1 $FILE2`
					if [ -n "$diffresult" ] ; #diffで結果が等しくなかった時のみ
					#if [ -z "" ] ;
					then
						gumtree "diff" $FILE1 $FILE2 > ./tmp.txt
						cat ./tmp.txt >> ./log.txt
						cat ./tmp.txt | grep "insert-node" | wc -l >> ./insert.txt
						cat ./tmp.txt | grep "delete-node" | wc -l >> ./delete.txt
						cat ./tmp.txt | grep "move-tree" | wc -l >> ./move.txt
						cat ./tmp.txt | grep "update-node" | wc -l >> ./update.txt
					fi
					break
				fi
			fi
		done
	fi 
done

cat ./insert.txt | awk '{sum+=$1}END{print "insert: "sum}'
cat ./delete.txt | awk '{sum+=$1}END{print "delete: "sum}'
cat ./move.txt | awk '{sum+=$1}END{print "move: "sum}'
cat ./update.txt | awk '{sum+=$1}END{print "update: "sum}'
